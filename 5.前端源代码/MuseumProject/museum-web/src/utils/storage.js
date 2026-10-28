// src/utils/storage.js
const USER_KEY = 'user'; // 全局定义一次，以后改也只改这一个地方

export const getUser = () => {
  const data = localStorage.getItem(USER_KEY);
  return data ? JSON.parse(data) : null;
};

export const getUserId = () => {
  const user = getUser();
  return user ? (user.userId || user.id) : null;
};