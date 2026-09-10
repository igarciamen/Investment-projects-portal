export interface UserInfo {
  id: number; 
  username: string;
  email: string;
  roles: string[];
  country?: string | null;
  occupation?: string | null;
  preferredContactLanguage?: string | null;
  organisationName?: string | null;
  organisationCountry?: string | null;
}