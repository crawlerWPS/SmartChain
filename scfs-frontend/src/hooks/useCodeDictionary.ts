import { useEffect, useMemo, useState } from 'react';
import { listCodeDictionaries, type CodeDictionaryItem } from '@/api/dictionary';

let dictionaryCache: CodeDictionaryItem[] | null = null;
let loadingPromise: Promise<CodeDictionaryItem[]> | null = null;

export function loadCodeDictionary(): Promise<CodeDictionaryItem[]> {
  if (dictionaryCache) return Promise.resolve(dictionaryCache);
  if (!loadingPromise) {
    loadingPromise = listCodeDictionaries()
      .then((items) => (dictionaryCache = items || []))
      .finally(() => { loadingPromise = null; });
  }
  return loadingPromise;
}

export function clearCodeDictionaryCache() {
  dictionaryCache = null;
  loadingPromise = null;
}

export function useCodeDictionary() {
  const [items, setItems] = useState<CodeDictionaryItem[]>(dictionaryCache || []);

  useEffect(() => {
    let active = true;
    loadCodeDictionary().then((data) => { if (active) setItems(data); }).catch(() => undefined);
    return () => { active = false; };
  }, []);

  const grouped = useMemo(() => {
    const result: Record<string, CodeDictionaryItem[]> = {};
    items.forEach((item) => { (result[item.codeType] ||= []).push(item); });
    return result;
  }, [items]);

  return {
    items,
    label: (type: string, code?: string | number | null) => {
      if (code == null) return '-';
      const raw = String(code);
      return grouped[type]?.find((item) => item.code === raw)?.codeValue || raw;
    },
    options: (type: string) => (grouped[type] || []).map((item) => ({
      label: item.codeValue,
      value: /^\d+$/.test(item.code) ? Number(item.code) : item.code,
    })),
  };
}
