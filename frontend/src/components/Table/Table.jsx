import './Table.css';

/**
 * Muestra una tabla reutilizable con cabeceras, filas de datos y un estado vacío.
 */
export function Table({
  headers = [],
  data = [],
  emptyMessage = 'No hay datos disponibles.',
}) {
  return (
    <div className="table-container">
      <table className="custom-table">
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
                  <td
                    key={cellIndex}
                    data-label={typeof headers[cellIndex] === 'string' ? headers[cellIndex] : undefined}
                  >
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
