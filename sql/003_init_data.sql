SET NAMES utf8mb4;

USE smart_law_firm_agent;

INSERT INTO sys_role (id, role_code, role_name, status) VALUES
(1, 'ADMIN', '管理员', 1),
(2, 'USER', '普通用户', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

-- BCrypt 明文均为 123456
INSERT INTO sys_user (id, username, password, nickname, phone, email, status, deleted) VALUES
(1, 'admin', '$2a$10$k0797Lg8CPSAzKQG7tR.AOs0hJANw.4lJhC0IvjgoA8flDSw2j4ge', '管理员', '13800000000', 'admin@example.com', 1, 0),
(2, 'user', '$2a$10$k0797Lg8CPSAzKQG7tR.AOs0hJANw.4lJhC0IvjgoA8flDSw2j4ge', '普通用户', '13900000000', 'user@example.com', 1, 0)
ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname), status = VALUES(status), deleted = 0;

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, permission, icon, sort_order, visible) VALUES
(1, 0, '首页', '/', 'Home', 'public:view', 'House', 1, 1),
(2, 0, '律所列表', '/law-firms', 'LawFirms', 'public:lawfirm:view', 'OfficeBuilding', 2, 1),
(3, 0, '律师列表', '/lawyers', 'Lawyers', 'public:lawyer:view', 'User', 3, 1),
(10, 0, '用户中心', '/user', 'UserLayout', 'user:view', 'Folder', 10, 1),
(20, 0, '管理后台', '/admin', 'AdminLayout', 'admin:view', 'Setting', 20, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component), permission = VALUES(permission), icon = VALUES(icon), sort_order = VALUES(sort_order), visible = VALUES(visible);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 10), (1, 20),
(2, 1), (2, 2), (2, 3), (2, 10)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

INSERT INTO law_firm (id, name, province_code, province_name, city, address, phone, email, description, specialties, license_no, status, deleted) VALUES
(1, '北京明德律师事务所', '110000', '北京市', '北京', '北京市朝阳区建国路88号现代法务中心18层', '010-88886666', 'contact@mingde-law.example', '综合型律师事务所，长期服务科技企业、家庭客户和城市更新项目，强调透明沟通与可执行方案。', '民事民法,公司企业,经济金融', 'LAW-BJ-2026-001', 1, 0),
(2, '上海知行律师事务所', '310000', '上海市', '上海', '上海市浦东新区陆家嘴环路1000号9层', '021-66668888', 'service@zhixing-law.example', '专注公司合规、涉外纠纷与金融争议解决，为跨境业务提供双语法律支持。', '涉外纠纷,公司企业,经济金融', 'LAW-SH-2026-002', 1, 0),
(3, '广东岭南律师事务所', '440000', '广东省', '广州', '广州市天河区珠江新城华夏路16号富力盈凯广场32层', '020-38889999', 'hello@lingnan-law.example', '立足粤港澳大湾区，擅长企业常年顾问、知识产权保护和劳动用工合规。', '公司企业,知识产权,劳动争议', 'LAW-GD-2026-003', 1, 0),
(4, '深圳前海湾律师事务所', '440000', '广东省', '深圳', '深圳市南山区前海深港基金小镇A座12层', '0755-26668888', 'qh@qianhai-law.example', '服务创业团队、金融科技公司和跨境电商，提供投融资、合同风控和争议解决方案。', '经济金融,公司企业,涉外纠纷', 'LAW-GD-2026-004', 1, 0),
(5, '黑龙江冰城律师事务所', '230000', '黑龙江省', '哈尔滨', '哈尔滨市南岗区红旗大街240号法务大厦6层', '0451-55667788', 'office@bingcheng-law.example', '深耕东北地区民商事诉讼、建设工程和劳动争议，熟悉本地司法实践。', '民事民法,建设工程,劳动争议', 'LAW-HLJ-2026-005', 1, 0),
(6, '浙江钱塘律师事务所', '330000', '浙江省', '杭州', '杭州市上城区钱江路58号数字法务港15层', '0571-88990011', 'contact@qiantang-law.example', '面向平台经济、互联网企业和知识产权密集型客户，提供合规、诉讼与交易支持。', '知识产权,公司企业,互联网合规', 'LAW-ZJ-2026-006', 1, 0),
(7, '四川锦城律师事务所', '510000', '四川省', '成都', '成都市高新区天府大道北段1700号环球中心E2座11层', '028-88997766', 'service@jincheng-law.example', '关注婚姻家事、房产纠纷与中小企业法律服务，强调调解优先和成本可控。', '民事民法,婚姻家事,房产纠纷', 'LAW-SC-2026-007', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), province_code = VALUES(province_code), province_name = VALUES(province_name), city = VALUES(city), address = VALUES(address), phone = VALUES(phone), email = VALUES(email), description = VALUES(description), specialties = VALUES(specialties), license_no = VALUES(license_no), status = 1, deleted = 0;

INSERT INTO lawyer (id, law_firm_id, name, gender, phone, email, category, title, experience_years, description, good_at, available_time_slots, status, deleted) VALUES
(1, 1, '张明', '男', '13810000001', 'zhangming@mingde-law.example', '民事民法', '高级合伙人', 12, '长期处理婚姻家事、合同违约和侵权责任案件，擅长把复杂事实拆成可执行的诉讼策略。', '合同纠纷、婚姻家事、侵权责任', '09:00,10:00,14:00,15:00', 1, 0),
(2, 1, '李清', '女', '13810000002', 'liqing@mingde-law.example', '公司企业', '合伙人律师', 9, '服务创业公司、制造企业和互联网企业，熟悉股权架构、劳动合规和合同体系搭建。', '股权设计、劳动合规、公司治理', '09:00,10:00,14:00,15:00', 1, 0),
(3, 2, '王远', '男', '13810000003', 'wangyuan@zhixing-law.example', '涉外纠纷', '资深律师', 10, '具备涉外合同与国际商事争议处理经验，可提供中英双语法律咨询。', '涉外合同、跨境争议、仲裁', '09:00,10:00,14:00,15:00', 1, 0),
(4, 2, '周若琳', '女', '13810000004', 'zhou@zhixing-law.example', '经济金融', '金融业务律师', 8, '曾参与银行、基金和融资租赁项目，关注交易结构、担保安排和争议预防。', '金融借款、担保纠纷、基金合规', '09:00,10:00,14:00,15:00', 1, 0),
(5, 3, '陈嘉明', '男', '13810000005', 'chen@lingnan-law.example', '公司企业', '主任律师', 13, '长期担任大湾区企业法律顾问，擅长把商业目标转化为合同和治理规则。', '常年顾问、公司治理、商务合同', '09:00,10:00,14:00,15:00', 1, 0),
(6, 3, '黄雅琪', '女', '13810000006', 'huang@lingnan-law.example', '知识产权', '知识产权律师', 7, '办理商标、著作权、不正当竞争案件，熟悉电商平台证据固定和维权流程。', '商标维权、著作权、不正当竞争', '09:00,10:00,14:00,15:00', 1, 0),
(7, 4, '林启航', '男', '13810000007', 'lin@qianhai-law.example', '经济金融', '投融资律师', 11, '专注创业融资、股权激励和金融科技合规，能协助企业建立从融资到退出的文件体系。', '投融资、股权激励、金融科技合规', '09:00,10:00,14:00,15:00', 1, 0),
(8, 4, '何婉宁', '女', '13810000008', 'he@qianhai-law.example', '涉外纠纷', '跨境业务律师', 8, '服务跨境电商与外贸企业，熟悉国际货物买卖、平台合规和跨境争议应对。', '跨境电商、国际贸易、平台合规', '09:00,10:00,14:00,15:00', 1, 0),
(9, 5, '赵思远', '男', '13810000009', 'zhao@bingcheng-law.example', '劳动争议', '资深律师', 9, '专注劳动争议与企业合规，熟悉东北地区劳动仲裁与法院裁判尺度。', '劳动仲裁、员工安置、规章制度', '09:00,10:00,14:00,15:00', 1, 0),
(10, 5, '孙雪', '女', '13810000010', 'sun@bingcheng-law.example', '民事民法', '诉讼律师', 6, '办理民间借贷、物业纠纷和建设工程小额争议，沟通细致，重视证据链完整性。', '民间借贷、物业纠纷、建设工程', '09:00,10:00,14:00,15:00', 1, 0),
(11, 6, '钱雨桐', '女', '13810000011', 'qian@qiantang-law.example', '知识产权', '合伙人律师', 10, '服务软件、文创和平台经济客户，擅长知识产权保护、内容合规和商业秘密管理。', '软件著作权、商业秘密、平台合规', '09:00,10:00,14:00,15:00', 1, 0),
(12, 6, '陆承泽', '男', '13810000012', 'lu@qiantang-law.example', '公司企业', '企业合规律师', 7, '帮助中小企业建立合同模板、授权审批和风险清单，适合需要长期规范化的客户。', '合同体系、企业合规、风险评估', '09:00,10:00,14:00,15:00', 1, 0),
(13, 7, '罗安然', '女', '13810000013', 'luo@jincheng-law.example', '婚姻家事', '家事律师', 8, '关注离婚、继承和未成年人权益保护，善于在诉讼与调解之间寻找稳妥方案。', '离婚纠纷、继承、家庭财产分割', '09:00,10:00,14:00,15:00', 1, 0),
(14, 7, '唐知行', '男', '13810000014', 'tang@jincheng-law.example', '房产纠纷', '民商事律师', 9, '处理房屋买卖、租赁和物业服务合同争议，熟悉成都本地不动产交易流程。', '房屋买卖、租赁纠纷、物业合同', '09:00,10:00,14:00,15:00', 1, 0)
ON DUPLICATE KEY UPDATE law_firm_id = VALUES(law_firm_id), name = VALUES(name), gender = VALUES(gender), phone = VALUES(phone), email = VALUES(email), category = VALUES(category), title = VALUES(title), experience_years = VALUES(experience_years), description = VALUES(description), good_at = VALUES(good_at), available_time_slots = VALUES(available_time_slots), status = 1, deleted = 0;

SET @first_workday := CASE
  WHEN DAYOFWEEK(CURRENT_DATE()) = 1 THEN DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)
  WHEN DAYOFWEEK(CURRENT_DATE()) = 7 THEN DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY)
  ELSE CURRENT_DATE()
END;
SET @second_workday := CASE
  WHEN DAYOFWEEK(DATE_ADD(@first_workday, INTERVAL 1 DAY)) = 7 THEN DATE_ADD(@first_workday, INTERVAL 3 DAY)
  ELSE DATE_ADD(@first_workday, INTERVAL 1 DAY)
END;

INSERT INTO appointment (id, user_id, law_firm_id, lawyer_id, law_firm_name, lawyer_name, appointment_time, remark, status, deleted) VALUES
(1001, 2, 5, 9, '黑龙江冰城律师事务所', '赵思远', TIMESTAMP(@first_workday, '09:00:00'), '演示：该时段已被预约', '已预约', 0)
ON DUPLICATE KEY UPDATE appointment_time = VALUES(appointment_time), remark = VALUES(remark), status = VALUES(status), deleted = 0;

INSERT INTO legal_case (id, user_id, lawyer_id, law_firm_id, case_no, title, case_type, description, case_time, status, deleted) VALUES
(1001, 2, 9, 5, 'CASE-DEMO-1001', '劳动仲裁庭前会议', '劳动争议', '演示：律师该时段有案件安排，不能接受新的预约。', TIMESTAMP(@second_workday, '14:00:00'), '处理中', 0)
ON DUPLICATE KEY UPDATE lawyer_id = VALUES(lawyer_id), law_firm_id = VALUES(law_firm_id), title = VALUES(title), case_type = VALUES(case_type), description = VALUES(description), case_time = VALUES(case_time), status = VALUES(status), deleted = 0;

INSERT INTO consultation (id, user_id, lawyer_id, law_firm_id, title, question, reply, status, deleted) VALUES
(1001, 2, 1, 1, '房屋租赁合同提前解除咨询', '房东要求提前解除租赁合同并扣除押金，想确认可以主张哪些权利。', '建议先固定租赁合同、付款凭证和沟通记录，再根据违约条款主张押金返还及损失赔偿。', '已回复', 0),
(1002, 2, 4, 2, '借款担保责任范围咨询', '亲友借款中本人作为保证人签字，想了解是否需要承担全部还款责任。', NULL, '待回复', 0)
ON DUPLICATE KEY UPDATE lawyer_id = VALUES(lawyer_id), law_firm_id = VALUES(law_firm_id), title = VALUES(title), question = VALUES(question), reply = VALUES(reply), status = VALUES(status), deleted = 0;

INSERT INTO notification (id, user_id, title, content, type, read_status, deleted) VALUES
(1001, 2, '预约提醒', '您预约的赵思远律师咨询已生成，请按时准备案件材料。', '预约', '未读', 0),
(1002, 2, '咨询已回复', '您的房屋租赁合同提前解除咨询已有律师回复，请进入我的咨询查看。', '咨询', '已读', 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), type = VALUES(type), read_status = VALUES(read_status), deleted = 0;
