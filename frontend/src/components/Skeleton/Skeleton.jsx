import './Skeleton.css';

export function Skeleton({ variant = 'list', count = 3, hasActions = true }) {
  const items = Array.from({ length: count }, (_, i) => i);

  if (variant === 'grid') {
    return (
      <div className="skeleton-grid" aria-hidden="true">
        {items.map((i) => (
          <div key={i} className="skeleton-card">
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
      <div className="skeleton-table" aria-hidden="true">
        {items.map((i) => (
          <div key={i} className={`skeleton-table-row ${!hasActions ? 'no-actions' : ''}`}>
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

  return (
    <div className="skeleton-list" aria-hidden="true">
      {items.map((i) => (
        <div key={i} className={`skeleton-block-card ${!hasActions ? 'no-actions' : ''}`}>
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
            {hasActions && <span className="skeleton-box skeleton-action" />}
          </div>
        </div>
      ))}
    </div>
  );
}