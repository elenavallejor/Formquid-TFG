import { getBlockItems } from '../../utils/blockBuilderUtils.js';
import { CatalogContentItem } from './CatalogContentItem.jsx';

export function CatalogBlockContent({ element }) {
  const items = Array.isArray(element.contentItems)
    ? element.contentItems
    : getBlockItems(element);
  const questionNumbers = items.reduce((numbers, item) => {
    numbers.push((numbers.at(-1) ?? 0) + (item.itemType === 'TEXT' ? 0 : 1));
    return numbers;
  }, []);

  return (
    <div className="block-questions-list catalog-detail-block-items">
      {items.map((item, index) => (
        <CatalogContentItem
          key={item.id || index}
          item={item}
          numbering={{ questionNumber: questionNumbers[index] }}
        />
      ))}
    </div>
  );
}
