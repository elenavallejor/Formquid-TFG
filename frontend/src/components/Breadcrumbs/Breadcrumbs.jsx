import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import { MdChevronRight } from 'react-icons/md';

export function Breadcrumbs({
  items = [],
  className = 'breadcrumb-nav',
  separator: Separator = MdChevronRight,
}) {
  return (
    <nav className={className} aria-label="Migas de pan">
      {items.map((item, index) => {
        const isCurrent = index === items.length - 1;

        return (
          <Fragment key={`${item.to || 'current'}-${item.label}`}>
            {index > 0 && <Separator aria-hidden="true" />}
            {item.to && !isCurrent ? (
              <Link to={item.to} state={item.state}>{item.label}</Link>
            ) : (
              <span aria-current={isCurrent ? 'page' : undefined}>{item.label}</span>
            )}
          </Fragment>
        );
      })}
    </nav>
  );
}
