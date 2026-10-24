const imageUrls = import.meta.glob('../../assets/images/gallery/*', {
  eager: true,
  query: '?url',
  import: 'default',
});

const descriptions = {
  '3dPINTING.png': 'Impresión 3D',
  'Aula.png': 'Aula',
  'Imatges-Posts-Web-30-min.png': 'Educación digital',
  'UN0462803.png': 'Aprendizaje',
  'aula2.png': 'Aula y tecnología',
  'gafas.png': 'Gafas inteligentes',
  'images.jpeg': 'Tecnología educativa',
  'jornada-ia-1200x600-1.png': 'Inteligencia artificial',
  'libro.jpg': 'Libro',
  'movil.jpeg': 'Teléfono móvil',
  'padres.jpeg': 'Familias',
  'realidad_aumentada.jpeg': 'Realidad aumentada',
  'realidad_virtual.jpg': 'Realidad virtual',
  'robotica.jpg': 'Robótica',
  'tablet.jpg': 'Tableta',
  'tele.jpeg': 'Televisión',
  'televisión.jpeg': 'Pantalla de televisión',
};

export const galleryImages = Object.entries(imageUrls)
  .map(([path, url]) => {
    const filename = path.split('/').pop();
    return { filename, url, label: descriptions[filename] ?? filename.replace(/[_-]/g, ' ') };
  })
  .sort((first, second) => first.label.localeCompare(second.label, 'es'));
