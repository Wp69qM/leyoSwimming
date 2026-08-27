export type ResignationStatus =
  'processing' | 'pending_audit' | 'approved' | 'rejected';

export type StudentHandleResult = 'transfer' | 'refund' | 'continue' | null;

export interface ResignationTicket {
  ticketId: string;
  ticketNo: string;
  coachId: string;
  coachName: string;
  coachPhone: string;
  reason: string;
  status: ResignationStatus;
  totalPackages: number;
  handledPackages: number;
  activeStudentCount: number;
  submittedAt: string;
  createdAt: string;
  coachJoinedAt?: string;
}

export interface ResignationTicketListData {
  items: ResignationTicket[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ResignationPackageItem {
  packageId: string;
  packageNo?: string;
  userId: string;
  userName: string;
  totalHours: number;
  availableCount: number;
  reservedCount: number;
  pricePerHour: number;
  action: StudentHandleResult;
  targetCoachId: string | null;
  targetCoachName?: string | null;
  targetCoachPhone?: string | null;
  refundAmount: number | null;
  handlerName?: string | null;
  handledAt?: string | null;
}

export interface ResignationChecklist {
  activeStudentsCleared: boolean;
  allActionsRegistered: boolean;
  scheduleCleared: boolean;
  settlementCompleted: boolean;
}

export interface ResignationCoachInfo {
  coachId: string;
  name: string;
  phone: string;
  status: number;
  submittedAt: string;
  joinedAt?: string;
}

export interface ResignationTicketDetail {
  ticketId: string;
  ticketNo: string;
  status: ResignationStatus;
  reason: string;
  totalPackages: number;
  handledPackages: number;
  scheduleCleared: boolean;
  settlementStatus: number;
  submittedAt: string;
  coach: ResignationCoachInfo;
  packages: ResignationPackageItem[];
  checklist: ResignationChecklist;
}

export interface ResignationListParams {
  page: number;
  pageSize: number;
  status?: ResignationStatus | '';
  keyword?: string;
  submitStartDate?: string;
  submitEndDate?: string;
}
