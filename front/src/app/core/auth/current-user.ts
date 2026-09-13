export interface CurrentUser {
  username: string;
  email: string | null;
  name: string | null;
  isAdmin: boolean;
}
