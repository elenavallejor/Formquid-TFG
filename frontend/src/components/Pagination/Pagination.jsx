import { MdChevronLeft, MdChevronRight } from 'react-icons/md';

import { Dropdown } from '../Dropdown/Dropdown.jsx';

import './Pagination.css';

function getVisiblePages(currentPage, totalPages) {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  const firstPage = Math.min(Math.max(currentPage - 2, 0), totalPages - 5);
  return Array.from({ length: 5 }, (_, index) => firstPage + index);
}

/** Controles de paginación para los listados de la aplicación. */
export function Pagination({
  ariaLabel = 'Paginación',
  currentPage,
  disabled = false,
  itemLabel = 'elementos',
  onPageChange,
  onPageSizeChange,
  pageSize = 1,
  pageSizeLabel = 'Elementos por página',
  pageSizeOptions = [],
  totalItems = 0,
  totalPages,
  variant = 'standard',
}) {
  const isCompact = variant === 'compact';
  const visiblePages = getVisiblePages(currentPage, totalPages);
  const startItem = totalItems > 0 ? currentPage * pageSize + 1 : 0;
  const endItem = Math.min((currentPage + 1) * pageSize, totalItems);

  return (
    <div className={`pagination pagination--${variant}`}>
      {isCompact ? (
        <div className="pagination-compact-controls" role="group" aria-label={ariaLabel}>
          <button
            type="button"
            disabled={disabled || currentPage === 0}
            onClick={() => onPageChange(currentPage - 1)}
          >
            <MdChevronLeft aria-hidden="true" />
            Anterior
          </button>
          <span className="pagination-compact-info">
            Página <strong>{currentPage + 1}</strong> de <strong>{totalPages}</strong>
          </span>
          <button
            type="button"
            disabled={disabled || currentPage >= totalPages - 1}
            onClick={() => onPageChange(currentPage + 1)}
          >
            Siguiente
            <MdChevronRight aria-hidden="true" />
          </button>
        </div>
      ) : (
        <>
          <div className="pagination-count">
            Mostrando {startItem} a {endItem} de {totalItems} {itemLabel}
          </div>

          <div className="pagination-pages" role="group" aria-label={ariaLabel}>
            <button
              type="button"
              disabled={disabled || currentPage === 0}
              aria-label="Página anterior"
              onClick={() => onPageChange(currentPage - 1)}
            >
              <MdChevronLeft aria-hidden="true" />
            </button>
            {visiblePages.map((page) => (
              <button
                key={page}
                type="button"
                className={currentPage === page ? 'active' : ''}
                disabled={disabled}
                aria-label={`Página ${page + 1}`}
                aria-current={currentPage === page ? 'page' : undefined}
                onClick={() => onPageChange(page)}
              >
                {page + 1}
              </button>
            ))}
            <button
              type="button"
              disabled={disabled || currentPage >= totalPages - 1}
              aria-label="Página siguiente"
              onClick={() => onPageChange(currentPage + 1)}
            >
              <MdChevronRight aria-hidden="true" />
            </button>
          </div>

          {pageSizeOptions.length > 0 && onPageSizeChange && (
            <Dropdown
              className="pagination-page-size"
              disabled={disabled}
              aria-label={pageSizeLabel}
              onChange={(event) => onPageSizeChange(Number(event.target.value))}
              value={pageSize}
            >
              {pageSizeOptions.map((option) => (
                <option key={option} value={option}>{option} por página</option>
              ))}
            </Dropdown>
          )}
        </>
      )}
    </div>
  );
}
