import { MdSearch } from 'react-icons/md';

import { Dropdown } from '../Dropdown/Dropdown.jsx';
import { Filter } from '../Filter/Filter.jsx';

import './SearchToolbar.css';

/** Barra compartida de búsqueda, categorías y ordenación para listados. */
export function SearchToolbar({
  ariaLabel = 'Búsqueda y filtros',
  searchLabel,
  searchPlaceholder = 'Buscar',
  searchValue,
  searchInputRef,
  onSearchChange,
  filterLabel = 'Filtrar',
  filterOptions,
  filterOptionsLabel = 'Filtrar por',
  filterOptionsControl,
  filterOptionsAllLabel,
  filterOptionsNoneLabel,
  filterOptionsSelectedCountLabel,
  selectedFilterValues,
  filterChoice,
  filterChoices,
  showFilter = false,
  onFilterChange,
  onFilterReset,
  sortAriaLabel = 'Ordenar resultados',
  sortOptions,
  sortValue,
  onSortChange,
}) {
  return (
    <section className="search-toolbar" aria-label={ariaLabel}>
      <div className="search-toolbar__search-group" role="search">
        <label className="search-toolbar__search">
          <span className="sr-only">{searchLabel}</span>
          <MdSearch aria-hidden="true" />
          <input
            ref={searchInputRef}
            type="search"
            placeholder={searchPlaceholder}
            value={searchValue}
            onChange={onSearchChange}
          />
        </label>

        {(showFilter || filterOptions?.length > 0 || filterChoice?.options?.length > 0 || filterChoices?.length > 0) && (
          <Filter
            label={filterLabel}
            options={filterOptions}
            optionsLabel={filterOptionsLabel}
            optionsControl={filterOptionsControl}
            optionsAllLabel={filterOptionsAllLabel}
            optionsNoneLabel={filterOptionsNoneLabel}
            optionsSelectedCountLabel={filterOptionsSelectedCountLabel}
            selectedValues={selectedFilterValues}
            choiceFilter={filterChoice}
            choiceFilters={filterChoices}
            onChange={onFilterChange}
            onReset={onFilterReset}
          />
        )}

      </div>

      {sortOptions?.length > 0 && (
        <div className="search-toolbar__sort">
          <span>Ordenar por:</span>
          <Dropdown
            aria-label={sortAriaLabel}
            value={sortValue}
            onChange={onSortChange}
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </Dropdown>
        </div>
      )}
    </section>
  );
}
