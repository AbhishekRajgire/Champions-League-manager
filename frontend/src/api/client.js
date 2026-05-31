import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

// Attach JWT from localStorage to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ucl_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Surface a clean error message.
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.statusText ||
      error.message ||
      'Request failed';
    return Promise.reject(new Error(message));
  }
);

export default api;
