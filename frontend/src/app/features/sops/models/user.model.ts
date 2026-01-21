export interface UserDto {
  id?: number;
  username: string;
  email: string;
  //Password not included for security
  fullName: string;
  role: 'ADMIN' | 'USER';
}
