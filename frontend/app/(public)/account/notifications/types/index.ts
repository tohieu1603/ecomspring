export interface Notification {
  id: number;
  title: string;
  content: string;
  referenceType?: string;
  referenceId?: string;
  read: boolean;
  createdAt: string;
}
