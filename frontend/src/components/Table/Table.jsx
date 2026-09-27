import './Table.css';

/**
 * Muestra una tabla reutilizable con cabeceras, filas de datos y un estado vacío.
 */
export function Table({
  headers = [],
  data = [],
  emptyMessage = 'No hay datos disponibles.',
  caption,
}) {
  const regionLabel = caption
    ? `${caption}. Tabla con desplazamiento horizontal`
    : 'Tabla con desplazamiento horizontal';

  return (
    <div
      className="table-container"
      role="region"
      aria-label={regionLabel}
      tabIndex={0}
    >
      <table className="custom-table">
        {caption && <caption className="sr-only">{caption}</caption>}

        {headers.length > 0 && (
          <thead>
            <tr>
              {headers.map((header) => (
                <th key={header} scope="col">
                  {header}
                </th>
              ))}
            </tr>
          </thead>
        )}

        <tbody>
          {data.length > 0 ? (
            data.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((cell, cellIndex) => (
                  <td key={cellIndex}>
                    {cell}
                  </td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td
                className="custom-table-empty"
                colSpan={Math.max(headers.length, 1)}
              >
                {emptyMessage}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
