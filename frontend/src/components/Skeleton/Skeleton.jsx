import './Skeleton.css';

export function Skeleton({
  variant = 'list',
  count = 3,
  hasActions = true,
  label = 'Cargando contenido…',
}) {
  const items = Array.from({ length: count }, (_, i) => i);

  if (variant === 'grid') {
    return (
      <div className="skeleton-grid" role="status" aria-live="polite">
        <span className="sr-only">{label}</span>
        {items.map((i) => (
          <div key={i} className="skeleton-card" aria-hidden="true">
            <div className="skeleton-row-between">
              <span className="skeleton-box skeleton-icon" />
              {hasActions && <span className="skeleton-box skeleton-action" />}
            </div>
            <div className="skeleton-content">
              <span className="skeleton-line skeleton-w-60" />
              <span className="skeleton-line skeleton-w-100" />
              <span className="skeleton-line skeleton-w-40" />
            </div>
            <div className="skeleton-row-between skeleton-border-top">
              <div className="skeleton-user">
                <span className="skeleton-box skeleton-avatar" />
                <span className="skeleton-line skeleton-w-80" />
              </div>
              <span className="skeleton-line skeleton-w-40" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (variant === 'table') {
    return (
      <div className="skeleton-table" role="status" aria-live="polite">
        <span className="sr-only">{label}</span>
        {items.map((i) => (
          <div key={i} className={`skeleton-table-row ${!hasActions ? 'no-actions' : ''}`} aria-hidden="true">
            <div>
              <span className="skeleton-line skeleton-w-60" />
              <span className="skeleton-line skeleton-w-40" />
            </div>
            <div>
              <span className="skeleton-line skeleton-w-80" />
            </div>
            <div>
              <span className="skeleton-line skeleton-w-40" />
            </div>
            <div>
              <span className="skeleton-line skeleton-w-80" />
            </div>
            {hasActions && (
              <div className="skeleton-right">
                <span className="skeleton-box skeleton-action" />
              </div>
            )}
          </div>
        ))}
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className="skeleton-compact-list" role="status" aria-live="polite">
        <span className="sr-only">{label}</span>
        {items.map((i) => (
          <div key={i} className="skeleton-compact-row" aria-hidden="true">
            <span className="skeleton-box skeleton-compact-icon" />
            <div className="skeleton-info">
              <span className="skeleton-line skeleton-w-60" />
              <span className="skeleton-line skeleton-w-100" />
            </div>
            {hasActions && <span className="skeleton-box skeleton-action" />}
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="skeleton-list" role="status" aria-live="polite">
      <span className="sr-only">{label}</span>
      {items.map((i) => (
        <div key={i} className={`skeleton-block-card ${!hasActions ? 'no-actions' : ''}`} aria-hidden="true">
          <div className="skeleton-main">
            <span className="skeleton-box skeleton-icon" />
            <div className="skeleton-info">
              <span className="skeleton-line skeleton-w-60" />
              <span className="skeleton-line skeleton-w-100" />
            </div>
          </div>

          <div className="skeleton-meta">
            <div className="skeleton-user">
              <span className="skeleton-box skeleton-avatar" />
              <span className="skeleton-line skeleton-w-80" />
            </div>
            <span className="skeleton-line skeleton-w-40" />
            <span className="skeleton-line skeleton-w-80" />
            <span className="skeleton-line skeleton-w-80" />
            {hasActions && <span className="skeleton-box skeleton-action" />}
          </div>
        </div>
      ))}
    </div>
  );
}
