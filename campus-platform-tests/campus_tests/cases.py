PUBLIC_API_CASES = [
    ("GET", "/auth/captcha", "captcha"),
    ("GET", "/svc/notice/page?pageNum=1&pageSize=5", "notice page"),
]


AUTHENTICATED_READ_CASES = [
    ("GET", "/auth/userInfo", "current user info"),
    ("GET", "/dashboard/overview", "dashboard overview"),
    ("GET", "/svc/notice/page?pageNum=1&pageSize=5", "notice page"),
    ("GET", "/svc/book/page?pageNum=1&pageSize=5", "book page"),
    ("GET", "/svc/repair/my?pageNum=1&pageSize=5", "my repairs"),
    ("GET", "/svc/card/record/my?pageNum=1&pageSize=5", "my card records"),
    ("GET", "/svc/dorm/building/list", "dorm buildings"),
    ("GET", "/svc/dorm/room/list", "dorm rooms"),
    ("GET", "/svc/dorm/allocation/my", "my dorm allocation"),
    ("GET", "/sys/message/inbox?pageNum=1&pageSize=5", "message inbox"),
    ("GET", "/sys/message/unread-count", "unread message count"),
]


ADMIN_READ_CASES = [
    ("GET", "/sys/user/page?pageNum=1&pageSize=5", "user management"),
    ("GET", "/sys/role/list", "role list"),
    ("GET", "/sys/menu/tree", "menu tree"),
    ("GET", "/sys/dict/type/list", "dict type list"),
    ("GET", "/sys/log/login?pageNum=1&pageSize=5", "login logs"),
    ("GET", "/edu/course/page?pageNum=1&pageSize=5", "course page"),
    ("GET", "/edu/score/page?pageNum=1&pageSize=5", "score page"),
    ("GET", "/svc/repair/page?pageNum=1&pageSize=5", "repair page"),
    ("GET", "/svc/card/record/page?pageNum=1&pageSize=5", "card record page"),
    ("GET", "/svc/dorm/allocation/list", "dorm allocation list"),
]


TEACHER_READ_CASES = [
    ("GET", "/edu/course/page?pageNum=1&pageSize=5", "teacher course page"),
    ("GET", "/edu/timetable/my", "teacher timetable"),
    ("GET", "/edu/attendance/session/active", "active attendance sessions"),
    ("GET", "/edu/leave/page?pageNum=1&pageSize=5", "leave approval page"),
    ("GET", "/edu/elective/drop-requests/pending", "pending drop requests"),
    ("GET", "/sys/message/my-courses", "teacher message courses"),
]


STUDENT_READ_CASES = [
    ("GET", "/edu/timetable/my", "student timetable"),
    ("GET", "/edu/elective/available", "available electives"),
    ("GET", "/edu/elective/my", "my electives"),
    ("GET", "/edu/attendance/my?pageNum=1&pageSize=5", "my attendance"),
    ("GET", "/edu/leave/my", "my leave records"),
    ("GET", "/edu/evaluation/my", "my evaluations"),
    ("GET", "/sys/message/my-teachers", "my teachers"),
]


FRONTEND_ROUTES = [
    "/login",
    "/register",
    "/dashboard",
    "/system/user",
    "/system/role",
    "/system/menu",
    "/system/dict",
    "/education/course",
    "/education/timetable",
    "/education/attendance",
    "/education/score",
    "/education/leave",
    "/campus/notice",
    "/campus/dorm",
    "/campus/repair",
    "/campus/card",
    "/campus/book",
    "/campus/message",
    "/statistics",
]
