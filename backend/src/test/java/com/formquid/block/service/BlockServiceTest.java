package com.formquid.block.service;

import com.formquid.block.dto.CreateBlockItemRequestDto;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ContentService contentService;

    @InjectMocks
    private BlockService blockService;

    @Test
    void validateDraft_rejectsNonConsecutivePositions() {
        var request = new CreateBlockRequestDto("Bloque", null, List.of(
            fixedQuestion(1, 11L),
            fixedQuestion(3, 12L)
        ));

        assertThatThrownBy(() -> blockService.validateDraft(request))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("consecutivas");
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
    void delete_allowsSuperadminWhenBlockIsUnused() {
        User admin = User.builder().id(2L).role(UserRole.SUPERADMIN).build();
        Block block = Block.builder().id(8L).scope(BlockScope.CATALOG).build();
        when(blockRepository.findById(8L)).thenReturn(Optional.of(block));
        when(blockRepository.countBySourceBlockId(8L)).thenReturn(0L);

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
