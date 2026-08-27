export interface CoachListItem {
  id: number;
  name: string;
  status: number;
  avatar: string | null;
  rating: string;
  yearsOfTeaching: number;
  teachingStrokes: string[];
  realTimeStatus: string;
}

export interface CoachListData {
  items: CoachListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface CoachDetailContact {
  phone: string | null;
  wechatQrUrl: string | null;
}

export interface CoachDetailCertificate {
  certType: string;
  imageUrl: string;
}

export interface CoachDetailPackage {
  id: number;
  name: string;
  packageMode: 'experience' | 'standard';
  price: string;
  totalHours: number;
  validDays: number;
  imageUrl: string | null;
}

export interface CoachDetailReview {
  id: number;
  avatar: string | null;
  nickname: string;
  rating: string;
  content: string;
  createdAt: string;
}

export interface CoachDetailAvailableTime {
  dayOfWeek: string;
  timeRanges: string[];
}

export interface CoachDetail {
  id: number;
  name: string;
  avatar: string | null;
  gender: number | null;
  age: number | null;
  status: number;
  rating: string;
  yearsOfTeaching: number;
  totalStudents: number;
  totalHours: number;
  teachingStrokes: string[];
  bio: string | null;
  referencePrice: string;
  contact: CoachDetailContact;
  certificates: CoachDetailCertificate[];
  packages: CoachDetailPackage[];
  reviews: CoachDetailReview[];
  reviewTags?: string[];
  availableTimes: CoachDetailAvailableTime[];
  realTimeStatus: string;
}

export interface UserPackageQualification {
  hasActivePackage: boolean;
  activePackageId: number | null;
  activeCoachId: number | null;
  hasExperiencePackage: boolean;
}

export type CoachSortType = 'rating' | 'price' | 'time';

export interface CoachListSearchParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  sortBy?: CoachSortType;
  sortOrder?: 'asc' | 'desc';
}
