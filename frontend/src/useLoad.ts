import { useCallback, useEffect, useState } from 'react';

export function useLoad<T>(loader: () => Promise<T>, initial: T) {
  const [data, setData] = useState<T>(initial);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    try {
      setError('');
      setData(await loader());
    } catch (e) {
      setError(`Не удалось загрузить данные: ${(e as Error).message}`);
    } finally {
      setLoaded(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loaded, error, reload, setData };
}
