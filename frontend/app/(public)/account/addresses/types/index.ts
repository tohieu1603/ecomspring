export interface Address {
  id: number;
  recipientName: string;
  recipientPhone: string;
  street: string;
  ward?: string;
  district?: string;
  city: string;
  country: string;
  isDefault: boolean;
}

/** Form mode: null = closed, "new" = empty insert, Address = edit existing. */
export type EditMode = Address | "new" | null;
