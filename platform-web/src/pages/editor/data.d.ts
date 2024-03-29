export type TableListItem = {
  key: number;
  disabled?: boolean;
  href: string;
  avatar: string;
  name: string;
  owner: string;
  desc: string;
  callNo: number;
  status: string;
  updatedAt: Date;
  createdAt: Date;
  progress: number;
};

export type TableListPagination = {
  total: number;
  pageSize: number;
  current: number;
};

export type TableListData = {
  list: TableListItem[];
  pagination: Partial<TableListPagination>;
};

export type CommitFile = {
  newPath: string;
  oldPath: string;
  newCode: string;
  oldCode: string;
};

export interface FilePaneItem extends CommitFile {
  key: string;
  editList: DiffEditDetailItems[];
  CriticalChange: HunkEntityItems | undefined;
  project: string;
}

export type DiffEditDetailItems = {
  beginA: number;
  beginB: number;
  endA: number;
  endB: number;
  lengthA: number;
  lengthB: number;
  empty: boolean;
  type: string;
};

export type CommitItem = {
  editList: DiffEditDetailItems[];
  filename: string;
  newPath: string;
  oldPath: string;
  match?: number;
  type?: string;
};

export type RegressionDetail = {
  regressionUuid: string;
  projectFullName?: string;
  bfc: string;
  bic: string;
  bicURL: string;
  bfcURL: string;
  bfcChangedFiles: CommitItem[];
  bicChangedFiles: CommitItem[];
  testCaseName: string;
  testFilePath: string;
  descriptionTxt: string;
};

export type RegressionCode = {
  regressionUuid: string;
  oldCode: string;
  newCode: string;
};

export type RegressionCriticalChangeDetail = {
  revissionName: 'bic' | 'bfc';
  hunkEntityPlusList: HunkEntityItems[];
};

export interface HunkEntityParams {
  newPath: string;
  oldPath: string;
  beginA: number;
  beginB: number;
  endA: number;
  endB: number;
  type: string;
}

export type HunkEntityItems = {
  reviewId: number;
  newPath: string;
  oldPath: string;
  beginA: number;
  beginB: number;
  endA: number;
  endB: number;
  type: string;
  tool?: string;
  accountName?: string;
  feedback?: string;
};

export type FeedbackList = {
  decorationKey: string[];
  revision: 'bic' | 'bfc';
  fileName: string;
  feedback: string;
  hunkData: HunkEntityParams;
};

export type CommentListItems = {
  commentId: string;
  accountName: string;
  regressionUuid: string;
  context: string;
  createTime: string;
};

export type CommentAPI = {
  // commentId: string;
  actions?: ReactNode[];
  author?: ReactNode;
  avatar?: ReactNode;
  children?: ReactNode;
  content?: ReactNode;
  datetime?: ReactNode;
};

export type BugTypeItems = {
  id: number;
  regressionUuid: string;
  bugTypeId: number;
  bugTypeName: string;
  agreeCount: number;
  disagreeCount: number;
  updateTime: Date;
  createdBy: string;
};
