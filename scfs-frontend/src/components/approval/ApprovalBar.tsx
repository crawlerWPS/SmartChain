/**
 * ApprovalBar - 双岗审批操作栏
 * - 自动区分经办/复核角色
 * - 隐藏无权限操作按钮
 * - 双岗隔离：maker ≠ checker
 */
import React from 'react';
import { Space, Button, message } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons';
import { useAccess } from '@umijs/max';
import { Permission } from '@/components/common/Permission';
import { isMakerOf, canApprove } from '@/access/access';

interface Props {
  makerId?: number;
  status: string; // 状态控制按钮显隐
  onSubmit?: () => void;
  onApprove?: () => void;
  onReject?: (reason: string) => void;
  submitPerm?: [string, string];
  approvePerm?: [string, string];
}

const ApprovalBar: React.FC<Props> = ({
  makerId,
  status,
  onSubmit,
  onApprove,
  onReject,
  submitPerm = ['rule', 'submit'],
  approvePerm = ['rule', 'approve'],
}) => {
  const { initialState } = useAccess();
  const currentUser = initialState?.currentUser;

  const canShowSubmit = status === 'DRAFT' || status === 'DRAFT_PENDING';
  const canShowApprove = status === 'PENDING';

  // 双岗隔离：经办人不能审批
  const isMyOwnChange = isMakerOf(makerId);
  const canApproveThis = canApprove(makerId);

  return (
    <Space>
      {canShowSubmit && (
        <Permission perm={submitPerm}>
          <Button type="primary" icon={<EditOutlined />} onClick={onSubmit}>
            提交审核
          </Button>
        </Permission>
      )}

      {canShowApprove && canApproveThis && !isMyOwnChange && (
        <Permission perm={approvePerm}>
          <Button type="primary" icon={<CheckOutlined />} onClick={onApprove}>
            审批通过
          </Button>
        </Permission>
      )}

      {canShowApprove && canApproveThis && !isMyOwnChange && (
        <Button danger icon={<CloseOutlined />} onClick={() => {
          const reason = window.prompt('请输入驳回原因');
          if (reason) onReject?.(reason);
        }}>
          驳回
        </Button>
      )}

      {isMyOwnChange && canShowApprove && (
        <span style={{ color: '#faad14', fontSize: 12 }}>
          当前变更由您提交，不能审批
        </span>
      )}
    </Space>
  );
};

export default ApprovalBar;
