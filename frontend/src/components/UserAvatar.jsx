import { useState } from 'react';

import { resolveProfilePhotoUrl } from '../api/apiClient.js';
import { getUserInitials } from '../utils/getUserInitials.js';

/** Muestra la foto guardada o las iniciales cuando no existe o no se puede cargar. */
export function UserAvatar({ name, imageKey, className = '' }) {
  const [failedKey, setFailedKey] = useState(null);

  return (
    <span className={className} aria-hidden="true">
      {imageKey && failedKey !== imageKey
        ? <img src={resolveProfilePhotoUrl(imageKey)} alt="" onError={() => setFailedKey(imageKey)} />
        : getUserInitials(name)}
    </span>
  );
}
