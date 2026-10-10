with open(r'f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeController.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()
lines[35] = '                throw ApiException.notFound("Đường dẫn không hợp lệ.");\n'
with open(r'f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeController.java', 'w', encoding='utf-8') as f:
    f.writelines(lines)

with open(r'f:\ltw\cinema\src\main\java\com\cinema\auth\AuthController.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()
lines[45] = '    // ------------------- Handlers -------------------\n'
lines[121] = '    // ------------------- Helpers -------------------\n'
with open(r'f:\ltw\cinema\src\main\java\com\cinema\auth\AuthController.java', 'w', encoding='utf-8') as f:
    f.writelines(lines)
