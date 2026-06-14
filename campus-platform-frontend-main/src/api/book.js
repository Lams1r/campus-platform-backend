import request from './request'

// 获取图书分页
export function getBookPage(params) {
  return request.get('/svc/book/page', { params })
}

// 新增图书
export function addBook(data) {
  return request.post('/svc/book', data)
}

// 修改图书
export function updateBook(data) {
  return request.put('/svc/book', data)
}

// 删除图书
export function deleteBook(id) {
  return request.delete(`/svc/book/${id}`)
}

// 借阅图书
export function borrowBook(bookId) {
  return request.post('/svc/book/borrow', null, { params: { bookId } })
}

// 归还图书
export function returnBook(borrowId) {
  return request.put(`/svc/book/return/${borrowId}`)
}

// 获取我的借阅
export function getMyBorrows(params) {
  return request.get('/svc/book/borrow/my', { params })
}

// 获取借阅分页（管理员）
export function getBorrowPage(params) {
  return request.get('/svc/book/borrow/page', { params })
}
