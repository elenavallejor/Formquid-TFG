const imageUrls = import.meta.glob('../../assets/images/gallery/*', {
  eager: true,
  query: '?url',
  import: 'default',
});

const descriptions = {
  'asistente_virtual.png': 'Asistente virtual',
  'aula.png': 'Aula',
  'clase.png': 'Clase',
  'gafas.png': 'Gafas inteligentes',
  'impresion_3d.png': 'Impresión 3D',
  'libro.jpg': 'Libro',
  'movil.jpeg': 'Teléfono móvil',
  'niño_pc.png': 'Niño con ordenador',
  'niño_tablet.jpeg': 'Niño con tableta',
  'niños_portatiles.jpeg': 'Aula y tecnología',
  'niños_robot.png': 'Robótica educativa',
  'padres.jpeg': 'Familias',
  'realidad_aumentada.jpeg': 'Realidad aumentada',
  'realidad_virtual.jpg': 'Realidad virtual',
  'robotica.jpg': 'Robótica',
  'tablet.jpg': 'Tableta',
  'tv.jpeg': 'Televisión',
  'tv_niños.jpeg': 'Pantalla de televisión',
};

export const galleryImages = Object.entries(imageUrls)
  .map(([path, url]) => {
    const filename = path.split('/').pop();
    return { filename, url, label: descriptions[filename] ?? filename.replace(/[_-]/g, ' ') };
  })
  .sort((first, second) => first.label.localeCompare(second.label, 'es'));
