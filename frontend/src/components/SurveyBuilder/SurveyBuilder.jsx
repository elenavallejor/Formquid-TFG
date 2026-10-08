import { TemplateBuilder } from '../TemplateBuilder/TemplateBuilder.jsx';

export function SurveyBuilder({
  data,
  onChange,
  onDropQuestionFromSidebar,
  onDropBlockFromSidebar,
  readOnly = false,
}) {
  return (
    <TemplateBuilder
      data={data}
      onChange={onChange}
      onDropQuestionFromSidebar={onDropQuestionFromSidebar}
      onDropBlockFromSidebar={onDropBlockFromSidebar}
      readOnly={readOnly}
      showGeneralInfo={false}
      structureTitle="ESTRUCTURA DE LA ENCUESTA"
      structureDescription="Combina preguntas, textos y bloques, y ordénalos como aparecerán en la encuesta."
      containerLabel="encuesta"
      includeEvaluation
    />
  );
}
