import { IoMdText } from "react-icons/io";
import "./TextContent.css";

/**
 * Define el contenido y el icono del texto informativo con estilos independientes del constructor.
 */
export function TextContent({ data = {}, showLabel = true }) {
  return (
    <div className="text-content-card">
      {showLabel && <span className="text-content-label">Texto informativo</span>}
      <p className="text-content-statement">{data?.textBody || ""}</p>
    </div>
  );
}

export function TextContentIcon() {
  return (
    <span className="text-content-icon" title="Texto informativo">
      <IoMdText aria-hidden="true" />
    </span>
  );
}
