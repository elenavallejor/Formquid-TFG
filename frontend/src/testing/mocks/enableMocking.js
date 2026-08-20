export async function enableMocking() {
  if (import.meta.env.MODE !== 'development') {
    return;
  }

  // mocks...
}