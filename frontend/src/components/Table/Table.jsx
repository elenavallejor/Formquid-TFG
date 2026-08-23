import './Table.css';

/**
 * Componente de Tabla reutilizable.
 * @param {Array<string>} headers - Lista con los nombres de las cabeceras.
 * @param {Array<Array<React.ReactNode>>} data - Matriz con el contenido de las filas y celdas.
 */
export function Table({ headers = [], data = [] }) {
  return (
    <div className="table-container">
      <table className="custom-table">
        {headers.length > 0 && (
          <thead>
            <tr>
              {headers.map((headerText, index) => (
                <th key={index}>{headerText}</th>
              ))}
            </tr>
          </thead>
        )}
        <tbody>
          {data.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td key={cellIndex}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}