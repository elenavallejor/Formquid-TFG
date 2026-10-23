import { LuEye, LuEyeClosed } from 'react-icons/lu';

import './PasswordVisibilityIcon.css';

export function PasswordVisibilityIcon({ visible }) {
  return visible
    ? <LuEyeClosed aria-hidden="true" />
    : <LuEye aria-hidden="true" />;
}
