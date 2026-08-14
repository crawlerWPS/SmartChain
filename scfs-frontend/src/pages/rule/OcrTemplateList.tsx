import React, { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, message, Upload, Alert, Descriptions } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { Permission } from '@/components/common/Permission';
import { CodeTag } from '@/components/common/CodeTag';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { analyzeOcrSample, createOcrTemplate, deleteOcrTemplate, listOcrTemplates, OcrTemplate, testOcrSample, updateOcrTemplate } from '@/api/ocrTemplate';

const fields = [
  ['buyerName','买方名称'],['sellerName','卖方名称'],['buyerUscc','买方信用代码'],['sellerUscc','卖方信用代码'],
  ['amount','金额'],['amountInWords','金额大写'],['transactionNo','发票号码/单据编号'],['commodity','商品名称'],
  ['contractDate','合同日期'],['orderDate','订单日期'],['invoiceDate','开票时间'],['logisticsDate','物流日期'],
  ['acceptanceDate','验收日期'],['paymentDate','付款日期'],['contractPeriod','合同期限'],['paymentTerm','付款条件'],
].map(([value,label])=>({value,label}));
const fieldsFor = (materialType?: string) => materialType === 'CONTRACT'
  ? fields.filter(field => !['buyerUscc','sellerUscc'].includes(field.value))
  : fields;
const modes = [{value:'ANCHOR_REGION',label:'锚点相对区域'},{value:'ABSOLUTE_REGION',label:'固定坐标区域'},{value:'FULL_TEXT',label:'全文正则'}];
const empty: OcrTemplate = { templateCode:'', templateName:'', materialType:'CONTRACT', priority:0, enabled:true, matchAnchors:[], fieldRules:[] };

const SampleDesigner: React.FC<{form:any}> = ({form}) => {
  const [sample,setSample]=useState<any>(), [page,setPage]=useState(1), [fieldCode,setFieldCode]=useState('buyerName');
  const [drawing,setDrawing]=useState<any>(), [testing,setTesting]=useState(false), [analyzing,setAnalyzing]=useState(false), [testResult,setTestResult]=useState<Record<string,any>>();
  const [uploadKey,setUploadKey]=useState(0);
  const current=sample?.pages?.find((p:any)=>p.page===page);
  const rules=Form.useWatch('fieldRules',form)||[];
  const materialType=Form.useWatch('materialType',form);
  const availableFields=fieldsFor(materialType);
  useEffect(()=>{if(!availableFields.some(field=>field.value===fieldCode))setFieldCode('buyerName');},[materialType]);
  const point=(e:React.MouseEvent<HTMLDivElement>)=>{const r=e.currentTarget.getBoundingClientRect();return{x:Math.max(0,Math.min(1,(e.clientX-r.left)/r.width)),y:Math.max(0,Math.min(1,(e.clientY-r.top)/r.height))};};
  const finish=(e:React.MouseEvent<HTMLDivElement>)=>{if(!drawing)return;const end=point(e),x=Math.min(drawing.x,end.x),y=Math.min(drawing.y,end.y),width=Math.abs(end.x-drawing.x),height=Math.abs(end.y-drawing.y);setDrawing(undefined);if(width<.005||height<.005)return;
    form.setFieldValue('fieldRules',[...rules,{fieldCode,extractMode:'ABSOLUTE_REGION',page,region:{x:+x.toFixed(4),y:+y.toFixed(4),width:+width.toFixed(4),height:+height.toFixed(4)},required:true}]);message.success('框选区域已换算为比例坐标并添加字段规则');};
  const upload=async(file:File)=>{if(analyzing)return false;setAnalyzing(true);try{setSample(await analyzeOcrSample(file));setPage(1);setTestResult(undefined);message.success('样本已完成分页渲染和OCR分析');}catch(e:any){message.error(e?.message||'样本识别失败，请查看服务日志');}finally{setAnalyzing(false);}return false;};
  const clearSample=()=>{setSample(undefined);setPage(1);setDrawing(undefined);setTestResult(undefined);setUploadKey(v=>v+1);form.setFieldValue('fieldRules',rules.filter((r:any)=>r.extractMode!=='ABSOLUTE_REGION'));message.success('已清除样本及该样本的框选区域，可重新上传');};
  const test=async()=>{if(!sample)return;setTesting(true);try{setTestResult(await testOcrSample(form.getFieldValue('fieldRules')||[],sample));}catch(e:any){message.error(e.message);}finally{setTesting(false);}};
  return <Card size="small" title="标准样本可视化配置" style={{marginBottom:16}} extra={<Space><Upload key={uploadKey} accept=".pdf,.png,.jpg,.jpeg" disabled={analyzing} showUploadList={false} beforeUpload={upload}><Button loading={analyzing} disabled={analyzing}>{sample?'重新上传样本':'上传标准材料样本'}</Button></Upload>{sample&&<Popconfirm title="清除当前样本？" description="同时移除该样本产生的固定坐标区域规则。" onConfirm={clearSample}><Button danger icon={<CloseCircleOutlined/>} disabled={analyzing}>清除样本</Button></Popconfirm>}<Button type="primary" disabled={!sample||analyzing} loading={testing} onClick={test}>测试识别</Button></Space>}>
    <Alert showIcon type="info" message={analyzing?'正在进行 PDF 分页渲染和 OCR 识别，请勿重复上传…':'上传样本后选择字段，在页面图片上拖拽矩形框；系统自动保存为 0～1 比例坐标。'} style={{marginBottom:12}}/>
    {sample&&<><Space style={{marginBottom:12}} wrap><Tag color="blue">样本：{sample.fileName||'标准样本'}</Tag><span>当前字段：</span><Select value={fieldCode} onChange={setFieldCode} options={availableFields} style={{width:180}}/><span>页面：</span><Select value={page} onChange={setPage} options={(sample.pages||[]).map((p:any)=>({value:p.page,label:`第 ${p.page} 页`}))} style={{width:110}}/><Tag>原始尺寸 {current?.width} × {current?.height}</Tag></Space>
      <div style={{display:'grid',gridTemplateColumns:'minmax(500px, 2fr) minmax(260px, 1fr)',gap:16}}>
        <div onMouseDown={e=>setDrawing(point(e))} onMouseUp={finish} style={{position:'relative',cursor:'crosshair',userSelect:'none',border:'1px solid #d9d9d9',lineHeight:0}}>
          {current&&<img src={current.image} draggable={false} style={{width:'100%',display:'block'}}/>}
          {rules.filter((r:any)=>r.page===page&&r.region).map((r:any,i:number)=><div key={i} title={r.fieldCode} style={{position:'absolute',left:`${r.region.x*100}%`,top:`${r.region.y*100}%`,width:`${r.region.width*100}%`,height:`${r.region.height*100}%`,border:'2px solid #ff4d4f',background:'rgba(255,77,79,.12)',lineHeight:'18px',color:'#cf1322',fontSize:12}}>{fields.find(f=>f.value===r.fieldCode)?.label}</div>)}
        </div>
        <div><Descriptions bordered size="small" column={1} title="测试识别结果">{testResult?Object.entries(testResult).map(([k,v])=><Descriptions.Item key={k} label={fields.find(f=>f.value===k)?.label||k}>{String(v||'-')}</Descriptions.Item>):<Descriptions.Item label="结果">点击“测试识别”查看字段提取值</Descriptions.Item>}</Descriptions></div>
      </div></>}
  </Card>;
};

const OcrTemplateList: React.FC = () => {
  const [data,setData]=useState<OcrTemplate[]>([]), [loading,setLoading]=useState(false), [editing,setEditing]=useState<OcrTemplate>();
  const [designerKey,setDesignerKey]=useState(0);
  const [form]=Form.useForm();
  const materialType=Form.useWatch('materialType',form);
  const availableFields=fieldsFor(materialType);
  const dictionary = useCodeDictionary();
  const load=async()=>{setLoading(true);try{setData(await listOcrTemplates()||[]);}catch(e:any){message.error(e.message);}finally{setLoading(false);}};
  useEffect(()=>{load();},[]);
  const open=(value:OcrTemplate)=>{const normalized=value.materialType==='CONTRACT'
    ? {...value,fieldRules:(value.fieldRules||[]).filter(rule=>!['buyerUscc','sellerUscc'].includes(rule.fieldCode))}
    : value;setDesignerKey(v=>v+1);setEditing(normalized);form.setFieldsValue(normalized);};
  const save=async()=>{const value=await form.validateFields();try{editing?.id?await updateOcrTemplate(editing.id,value):await createOcrTemplate(value);message.success('保存成功');setEditing(undefined);form.resetFields();load();}catch(e:any){message.error(e.message);}};
  const remove=async(id:number)=>{try{await deleteOcrTemplate(id);message.success('删除成功');load();}catch(e:any){message.error(e.message);}};
  const columns=[
    {title:'模板编号',dataIndex:'templateCode'},
    {title:'模板名称',dataIndex:'templateName'},
    {title:'材料类型',dataIndex:'materialType',render:(v:string)=><CodeTag type="MATERIAL_TYPE" code={v}/>},
    {title:'优先级',dataIndex:'priority'},
    {title:'字段数',dataIndex:'fieldRules',render:(v:any[])=>v?.length||0},
    {title:'状态',dataIndex:'enabled',render:(v:boolean)=><Tag color={v?'green':'default'}>{v?'启用':'停用'}</Tag>},
    {title:'操作',render:(_:any,r:OcrTemplate)=><Space>
      <Permission perm={['RULE','update']}><a onClick={()=>open(r)}><EditOutlined/> 修改</a></Permission>
      <Permission perm={['RULE','delete']}><Popconfirm title="确认删除该识别模板？" onConfirm={()=>remove(r.id!)}><a style={{color:'#ff4d4f'}}><DeleteOutlined/> 删除</a></Popconfirm></Permission>
    </Space>},
  ];
  return <Card title="OCR识别模板" extra={<Permission perm={['RULE','create']}><Button type="primary" icon={<PlusOutlined/>} onClick={()=>open({...empty})}>新建模板</Button></Permission>}>
    <Table rowKey="id" loading={loading} dataSource={data} columns={columns}/>
    <Modal width={1100} title={editing?.id?'修改OCR识别模板':'新建OCR识别模板'} open={!!editing} onOk={save} onCancel={()=>{setEditing(undefined);form.resetFields();}}>
      <Form form={form} layout="vertical">
        <Space align="start" wrap>
          <Form.Item name="templateCode" label="唯一模板编号" rules={[{required:true},{pattern:/^[A-Za-z0-9_-]{2,64}$/,message:'仅支持字母、数字、下划线和短横线'}]}><Input placeholder="如 OCR_CONTRACT_STANDARD" style={{width:220}} onInput={e=>{e.currentTarget.value=e.currentTarget.value.toUpperCase();}}/></Form.Item>
          <Form.Item name="templateName" label="模板名称" rules={[{required:true}]}><Input style={{width:220}}/></Form.Item>
          <Form.Item name="materialType" label="材料类型" rules={[{required:true}]}><Select style={{width:180}} options={dictionary.options('MATERIAL_TYPE')} onChange={type=>{
            if(type==='CONTRACT')form.setFieldValue('fieldRules',(form.getFieldValue('fieldRules')||[]).filter((rule:any)=>!['buyerUscc','sellerUscc'].includes(rule.fieldCode)));
          }}/></Form.Item>
          <Form.Item name="priority" label="优先级"><InputNumber min={0}/></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch/></Form.Item>
        </Space>
        <Form.Item name="matchAnchors" label="模板匹配关键词"><Select mode="tags" placeholder="输入关键词后回车，如：增值税电子发票"/></Form.Item>
        <SampleDesigner key={designerKey} form={form}/>
        <Form.List name="fieldRules">
          {(items,{add,remove})=><>
            <Table pagination={false} dataSource={items} rowKey="key" columns={[
              {title:'字段',render:(_:any,f:any)=><Form.Item name={[f.name,'fieldCode']} rules={[{required:true}]}><Select style={{width:140}} options={availableFields}/></Form.Item>},
              {title:'提取方式',render:(_:any,f:any)=><Form.Item name={[f.name,'extractMode']} rules={[{required:true}]}><Select style={{width:140}} options={modes}/></Form.Item>},
              {title:'页',render:(_:any,f:any)=><Form.Item name={[f.name,'page']}><InputNumber min={1} style={{width:55}}/></Form.Item>},
              {title:'关键词/正则/校验',render:(_:any,f:any)=><Space direction="vertical"><Form.Item name={[f.name,'anchors']}><Select mode="tags" style={{width:190}} placeholder="定位关键词"/></Form.Item><Form.Item name={[f.name,'pattern']}><Input style={{width:190}} placeholder="提取或校验正则"/></Form.Item><Space><Form.Item name={[f.name,'required']} valuePropName="checked"><Switch size="small"/></Form.Item><span>必填</span><Form.Item name={[f.name,'minConfidence']}><InputNumber min={0} max={100} placeholder="最低置信度" style={{width:100}}/></Form.Item></Space></Space>},
              {title:'归一坐标 x / y / 宽 / 高',render:(_:any,f:any)=><Space size={4}>{['x','y','width','height'].map(k=><Form.Item key={k} name={[f.name,'region',k]}><InputNumber min={k==='x'||k==='y'?-1:0} max={1} step={0.01} placeholder={k} style={{width:72}}/></Form.Item>)}</Space>},
              {title:'操作',render:(_:any,f:any)=><a onClick={()=>remove(f.name)}>移除</a>},
            ]}/>
            <Button style={{marginTop:12}} onClick={()=>add({extractMode:'ANCHOR_REGION',page:1,region:{x:0,y:-0.02,width:.4,height:.06}})}>添加字段规则</Button>
          </>}
        </Form.List>
      </Form>
    </Modal>
  </Card>;
};
export default OcrTemplateList;
