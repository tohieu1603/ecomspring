/**
 * Shape definitions shared by the product-detail page, hook, services, utils
 * and components. Keep this file dependency-free so utils can be unit-tested
 * without pulling React.
 */

/** Variant attribute as returned by catalog-service. Both legacy + canon names. */
export interface BeAttr {
  attrId?: number;
  attrCode?: string;
  attrName?: string;
  valId?: number | null;
  valText?: string;
  attrValId?: number | null;
  val?: string;
}

export interface BeVariant {
  id: number;
  sku: string;
  price: string;
  salePrice?: string | null;
  image?: string | null;
  quantity?: number;
  status?: string;
  attrs?: BeAttr[];
}

export interface BeProduct {
  id: number;
  name: string;
  slug: string;
  description?: string;
  brand?: string;
  categoryId?: number | null;
  thumbnail?: string;
  images?: string[];
  status?: string;
  variants?: BeVariant[];
}

/** One distinct attribute (e.g. "Color") with all of its possible values. */
export interface AttrGroup {
  attrId: number;
  attrCode: string;
  attrName: string;
  values: AttrValue[];
}

export interface AttrValue {
  valId: number;
  valText: string;
  /** Representative image for COLOR values, undefined for SIZE etc. */
  image?: string;
}

/** Picked = attrId → attrValId. */
export type PickedAttrs = Record<number, number>;

export type AddToBagResult =
  | { ok: true }
  | { ok: false; reason: "auth" | "stock" | "deleted" | "inactive" | "validation" | "unknown"; message: string };
