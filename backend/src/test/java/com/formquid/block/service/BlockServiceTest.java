package com.formquid.block.service;

import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.service.ContentService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import com.formquid.common.Category;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {
    @Test
    void createAndUpdate_dispatchItemsAndNormalizeMetadata() {
        User owner=User.builder().id(1L).name("Autora").role(UserRole.EDITOR).build();
        var draft=new QuestionDraftDto("Pregunta",null,QuestionType.TEXT,null,null,null,List.of(),List.of());
        var request=new CreateBlockRequestDto(" Bloque "," descripción ",List.of(
            new CreateBlockItemRequestDto(ContentItemType.TEXT,1,null,null,null," Intro "),
            fixedQuestion(2,11L),
            new CreateBlockItemRequestDto(ContentItemType.QUESTION,3,false,draft,null,null)),Category.OTROS);
        when(blockRepository.save(any())).thenAnswer(i -> { Block b=i.getArgument(0); b.setId(8L); return b; });
        var result=blockService.createCatalogBlock(request,owner);
        assertThat(result.name()).isEqualTo("Bloque");
        assertThat(result.description()).isEqualTo("descripción");
        assertThat(result.scope()).isEqualTo(BlockScope.CATALOG);
        verify(contentService).addTextToBlock(any(),any());
        verify(contentService).addFixedQuestionToBlock(any(),any(),eq(owner));
        verify(contentService).addLocalQuestionToBlock(any(),any(),eq(owner));
        Block existing=Block.builder().id(8L).scope(BlockScope.CATALOG).createdBy(owner).build();
        when(blockRepository.findById(8L)).thenReturn(Optional.of(existing));
        assertThat(blockService.updateCatalogBlock(8L,request,owner).name()).isEqualTo("Bloque");
        assertThat(existing.getUpdatedAt()).isNotNull();
        verify(contentService).deleteBlockContent(existing);
        assertThat(blockService.findCatalogBlock(8L).id()).isEqualTo(8L);
    }

    @Test
    void findCatalogLists_mapsBuilderContentAndSearch() {
        User owner=User.builder().id(1L).build();
        when(blockRepository.findCatalogSummaries(any(),any(),anyBoolean(),anyString(),anyBoolean(),anyBoolean(),anyBoolean(),any(),any())).thenReturn(Page.empty());
        assertThat(blockService.findCatalogBlocks(0,10,owner,false," Buscar ",Set.of(CatalogSearchField.TITLE),null,null).content()).isEmpty();
        blockService.findCatalogBlocks(0,10,owner,false,null,null,null,null);
        verify(blockRepository).findCatalogSummaries(eq(BlockScope.CATALOG),eq(1L),eq(false),eq("buscar"),eq(true),eq(false),eq(false),isNull(),any());
        Block block=Block.builder().id(8L).name("Bloque").scope(BlockScope.CATALOG).build();
        when(blockRepository.findCatalogBlocksWithDetails(eq(BlockScope.CATALOG),isNull(),any())).thenReturn(new PageImpl<>(List.of(block)));
        when(contentService.findBlocksContent(List.of(block))).thenReturn(Map.of());
        assertThat(blockService.findCatalogBlocksForBuilder(0,10,null).content()).singleElement().satisfies(b -> {
            assertThat(b.name()).isEqualTo("Bloque");
            assertThat(b.contentItems()).isEmpty();
        });
    }

    @Test
    void catalogOperations_rejectSnapshotsAndMissingBlocks() {
        Block snapshot=Block.builder().id(8L).scope(BlockScope.SNAPSHOT).build();
        when(blockRepository.findById(8L)).thenReturn(Optional.of(snapshot));
        assertThatThrownBy(() -> blockService.findCatalogBlock(8L)).hasMessage("Bloque de catálogo no encontrado");
        assertThatThrownBy(() -> blockService.deleteCatalogBlock(8L,null)).hasMessage("Bloque de catálogo no encontrado");
        assertThatThrownBy(() -> blockService.updateCatalogBlock(8L,null,null)).hasMessage("Bloque de catálogo no encontrado");
        assertThatThrownBy(() -> blockService.getBlock(99L)).hasMessage("Bloque no encontrado");
    }

    @Test
    void validateDraft_rejectsMissingAndOversizedNamesAndNestedBlocks() {
        assertThatThrownBy(() -> blockService.validateDraft(null)).hasMessage("El bloque necesita un nombre");
        assertThatThrownBy(() -> blockService.validateDraft(new CreateBlockRequestDto("x".repeat(151),null,List.of(),null)))
            .hasMessageContaining("150 caracteres");
        assertThatThrownBy(() -> blockService.validateItems(List.of())).hasMessageContaining("al menos una pregunta");
        assertThatThrownBy(() -> blockService.validateItems(List.of(fixedQuestion(1,11L),new CreateBlockItemRequestDto(ContentItemType.BLOCK,2,null,null,null,null))))
            .hasMessage("Un bloque no puede contener otros bloques");
        assertThatThrownBy(() -> blockService.validateItems(List.of(fixedQuestion(1,11L),fixedQuestion(1,12L))))
            .hasMessageContaining("duplicadas");
    }

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ContentService contentService;
    @Mock
    private CategoryCatalogService categoryCatalogService;

    @InjectMocks
    private BlockService blockService;

    @Test
    void validateDraft_rejectsNonConsecutivePositions() {
        var request = new CreateBlockRequestDto("Bloque", null, List.of(
            fixedQuestion(1, 11L),
            fixedQuestion(3, 12L)
        ), null);

        assertThatThrownBy(() -> blockService.validateDraft(request))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("consecutivas");
    }

    @Test
    void createRequiresCategory() {
        var request = new CreateBlockRequestDto("Bloque", null,
            List.of(fixedQuestion(1, 11L)), null);

        assertThatThrownBy(() -> blockService.createCatalogBlock(request, null))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("categoría del bloque es obligatoria");
    }

    @Test
    void delete_rejectsEditorWhoDidNotCreateTheBlock() {
        User creator = User.builder().id(1L).build();
        User editor = User.builder().id(2L).role(UserRole.EDITOR).build();
        Block block = Block.builder().id(8L).scope(BlockScope.CATALOG).createdBy(creator).build();
        when(blockRepository.findById(8L)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> blockService.deleteCatalogBlock(8L, editor))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("propios bloques");
        verify(blockRepository, never()).delete(block);
    }

    @Test
    void delete_allowsSuperadminToRemoveCatalogBlock() {
        User admin = User.builder().id(2L).role(UserRole.SUPERADMIN).build();
        Block block = Block.builder().id(8L).scope(BlockScope.CATALOG).build();
        when(blockRepository.findById(8L)).thenReturn(Optional.of(block));

        blockService.deleteCatalogBlock(8L, admin);

        verify(contentService).deleteBlockContent(block);
        verify(blockRepository).delete(block);
    }

    private CreateBlockItemRequestDto fixedQuestion(int position, Long questionId) {
        return new CreateBlockItemRequestDto(
            ContentItemType.QUESTION, position, true, null, questionId, null
        );
    }
}
