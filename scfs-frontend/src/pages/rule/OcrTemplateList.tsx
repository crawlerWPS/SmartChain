import React, { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, message } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Permission } from '@/components/common/Permission';
import { createOcrTemplate, deleteOcrTemplate, listOcrTemplates, OcrTemplate, updateOcrTemplate } from '@/api/ocrTemplate';

const fields = [
  ['buyerName','买方名称'],['sellerName','卖方名称'],['buyerUscc','买方信用代码'],['sellerUscc','卖方信用代码'],
  ['amount','金额'],['transactionNo','合同/发票号码'],['commodity','商品名称'],
].map(([value,label])=>({value,label}));
const modes = [{value:'ANCHOR_REGION',label:'锚点相对区域'},{value:'ABSOLUTE_REGION',label:'固定坐标区域'},{value:'FULL_TEXT',label:'全文正则'}];
const empty: OcrTemplate = { templateName:'', materialType:'CONTRACT', priority:0, enabled:true, matchAnchors:[], fieldRules:[] };

const OcrTemplateList: React.FC = () => {
  const [data,setData]=useState<OcrTemplate[]>([]), [loading,setLoading]=useState(false), [editing,setEditing]=useState<OcrTemplate>();
  const [form]=Form.useForm();
  const load=async()=>{setLoading(true);try{setData(await listOcrTemplates()||[]);}catch(e:any){message.error(e.message);}finally{setLoading(false);}};
  useEffect(()=>{load();},[]);
  const open=(value:OcrTemplate)=>{setEditing(value);form.setFieldsValue(value);};
  const save=async()=>{const value=await form.validateFields();try{editing?.id?await updateOcrTemplate(editing.id,value):await createOcrTemplate(value);message.success('保存成功');setEditing(undefined);form.resetFields();load();}catch(e:any){message.error(e.message);}};
  const remove=async(id:number)=>{try{await deleteOcrTemplate(id);message.success('删除成功');load();}catch(e:any){message.error(e.message);}};
  const columns=[
    {title:'模板名称',dataIndex:'templateName'},
    {title:'材料类型',dataIndex:'materialType',render:(v:string)=>v==='CONTRACT'?'贸易合同':'发票'},
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
          <Form.Item name="templateName" label="模板名称" rules={[{required:true}]}><Input style={{width:220}}/></Form.Item>
          <Form.Item name="materialType" label="材料类型" rules={[{required:true}]}><Select style={{width:160}} options={[{value:'CONTRACT',label:'贸易合同'},{value:'INVOICE',label:'发票'}]}/></Form.Item>
          <Form.Item name="priority" label="优先级"><InputNumber min={0}/></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch/></Form.Item>
        </Space>
        <Form.Item name="matchAnchors" label="模板匹配关键词"><Select mode="tags" placeholder="输入关键词后回车，如：增值税电子发票"/></Form.Item>
        <Form.List name="fieldRules">
          {(items,{add,remove})=><>
            <Table pagination={false} dataSource={items} rowKey="key" columns={[
              {title:'字段',render:(_:any,f:any)=><Form.Item name={[f.name,'fieldCode']} rules={[{required:true}]}><Select style={{width:140}} options={fields}/></Form.Item>},
              {title:'提取方式',render:(_:any,f:any)=><Form.Item name={[f.name,'extractMode']} rules={[{required:true}]}><Select style={{width:140}} options={modes}/></Form.Item>},
              {title:'页',render:(_:any,f:any)=><Form.Item name={[f.name,'page']}><InputNumber min={1} style={{width:55}}/></Form.Item>},
              {title:'锚点/正则',render:(_:any,f:any)=><Space direction="vertical"><Form.Item name={[f.name,'anchors']}><Select mode="tags" style={{width:190}} placeholder="锚点关键词"/></Form.Item><Form.Item name={[f.name,'pattern']}><Input style={{width:190}} placeholder="全文正则（可选）"/></Form.Item></Space>},
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
