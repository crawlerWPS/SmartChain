import { request } from '@umijs/max';

export type OcrFieldRule = {
  fieldCode: string; extractMode: 'ABSOLUTE_REGION'|'ANCHOR_REGION'|'FULL_TEXT'; page?: number;
  anchors?: string[]; pattern?: string; direction?: string;
  region?: { x: number; y: number; width: number; height: number };
  required?: boolean; minConfidence?: number;
};
export type OcrTemplate = { id?: number; templateCode: string; templateName: string; materialType: string; enterpriseId?: number; priority: number; enabled: boolean; matchAnchors: string[]; fieldRules: OcrFieldRule[] };
export const listOcrTemplates = (materialType?: string): Promise<OcrTemplate[]> => request('/ocr-templates', { params: { materialType } });
export const createOcrTemplate = (data: OcrTemplate) => request('/ocr-templates', { method:'POST', data });
export const updateOcrTemplate = (id: number, data: OcrTemplate) => request(`/ocr-templates/${id}`, { method:'PUT', data });
export const deleteOcrTemplate = (id: number) => request(`/ocr-templates/${id}`, { method:'DELETE' });
export const analyzeOcrSample = (file: File): Promise<any> => {
  const data = new FormData(); data.append('file', file);
  return request('/ocr-templates/sample/analyze', { method:'POST', data, timeout: 300000 });
};
export const testOcrSample = (fieldRules: OcrFieldRule[], sample: any): Promise<Record<string, any>> =>
  request('/ocr-templates/sample/test', { method:'POST', data: { fieldRules, sample } });
