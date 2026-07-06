SET NAMES utf8mb4;

USE smart_law_auth;

INSERT INTO sys_role (id, role_code, role_name) VALUES
  (1, 'ADMIN', '管理员'),
  (2, 'USER', '普通用户'),
  (3, 'LAWYER', '律师')
ON DUPLICATE KEY UPDATE
  role_code = VALUES(role_code),
  role_name = VALUES(role_name),
  deleted = 0;

INSERT INTO sys_user (id, username, password, nickname, phone, email, status) VALUES
  (1, 'admin', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '管理员', '13800000001', 'admin@smart-law.example', 1),
  (2, 'user', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '普通用户', '13800000002', 'user@smart-law.example', 1),
  (3, 'lawyer', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '林启航', '13800001111', 'linqihang@example.com', 1),
  (4, 'lawyer_chen', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '陈若澜', '13800002222', 'chenruolan@example.com', 1),
  (5, 'lawyer_zhou', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '周明砚', '13800003333', 'zhoumingyan@example.com', 1),
  (6, 'lawyer_he', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '何知微', '13800004444', 'hezhiwei@example.com', 1),
  (7, 'lawyer_tan', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '谭思远', '13800005555', 'tansiyuan@example.com', 1),
  (8, 'lawyer_liang', '$2a$10$0PVdICMjn0spLgByfQ0ypegaxyGzVJtEU9qOzuCgKDyk3ywZvsReS', '梁雪宁', '13800006666', 'liangxuening@example.com', 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  phone = VALUES(phone),
  email = VALUES(email),
  status = VALUES(status),
  deleted = 0;

INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (4, 3),
  (5, 3),
  (6, 3),
  (7, 3),
  (8, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

USE smart_law_lawyer;

INSERT INTO law_firm (id, name, province_code, province_name, city, address, phone, email, specialties, description, license_no, status) VALUES
  (1, '深圳前海湾律师事务所', '440000', '广东省', '深圳市', '南山区前海深港合作区桂湾三路18号', '0755-88886666', 'contact@qianhai-law.example', '劳动争议,合同纠纷,公司法务', '专注企业合规、劳动争议和商业合同纠纷的综合型律所，服务互联网、制造业和跨境贸易客户。', 'GD-2026-0001', 1),
  (2, '广州越秀衡正律师事务所', '440000', '广东省', '广州市', '越秀区东风中路515号', '020-88990011', 'service@yuexiu-law.example', '婚姻家事,房产纠纷,债权债务', '以民商事诉讼和家事财富规划为核心业务，强调证据梳理、风险评估与调解方案设计。', 'GD-2026-0002', 1),
  (3, '哈尔滨松北明理律师事务所', '230000', '黑龙江省', '哈尔滨市', '松北区创新一路88号', '0451-55667788', 'hello@songbei-law.example', '交通事故,劳动工伤,刑事辩护', '深耕东北地区民生争议、工伤赔偿与刑事合规业务，具备本地法院办案经验。', 'HLJ-2026-0001', 1),
  (4, '北京朝阳知行律师事务所', '110000', '北京市', '北京市', '朝阳区建国门外大街22号', '010-66778899', 'office@zhixing-law.example', '知识产权,互联网纠纷,公司治理', '面向科技企业和内容平台，提供知识产权保护、平台治理和投融资合规法律服务。', 'BJ-2026-0001', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  province_code = VALUES(province_code),
  province_name = VALUES(province_name),
  city = VALUES(city),
  address = VALUES(address),
  phone = VALUES(phone),
  email = VALUES(email),
  specialties = VALUES(specialties),
  description = VALUES(description),
  license_no = VALUES(license_no),
  status = VALUES(status),
  deleted = 0;

INSERT INTO lawyer (id, user_id, law_firm_id, name, gender, phone, email, category, title, experience_years, description, good_at, available_time_slots, status) VALUES
  (1, 3, 1, '林启航', '男', '13800001111', 'linqihang@example.com', '劳动争议', '高级合伙人', 9, '林启航律师长期服务互联网与制造业企业，同时代理劳动者维权案件，擅长在证据链和调解策略之间取得平衡。', '劳动仲裁,违法解除,竞业限制', '周一至周五 09:00-17:00', 1),
  (2, 4, 1, '陈若澜', '女', '13800002222', 'chenruolan@example.com', '合同纠纷', '专职律师', 6, '陈若澜律师处理过大量买卖合同、服务合同及股权合作争议，擅长从合同履行记录中还原关键事实。', '买卖合同,服务合同,股权合作', '周一至周五 09:00-17:00', 1),
  (3, 5, 2, '周明砚', '男', '13800003333', 'zhoumingyan@example.com', '婚姻家事', '合伙人', 11, '周明砚律师长期办理离婚、继承和共有财产分割案件，注重谈判策略与未成年子女权益保护。', '离婚纠纷,继承纠纷,财产分割', '周一至周五 09:00-17:00', 1),
  (4, 6, 2, '何知微', '女', '13800004444', 'hezhiwei@example.com', '房产纠纷', '专职律师', 7, '何知微律师熟悉商品房买卖、租赁合同和物业服务纠纷，能够为业主和企业客户提供诉前方案。', '房屋买卖,租赁纠纷,物业纠纷', '周一至周五 09:00-17:00', 1),
  (5, 7, 3, '谭思远', '男', '13800005555', 'tansiyuan@example.com', '交通事故', '主任律师', 13, '谭思远律师擅长交通事故责任认定、伤残赔偿和保险理赔争议，熟悉东北地区司法鉴定流程。', '交通事故,保险理赔,人身损害', '周一至周五 09:00-17:00', 1),
  (6, 8, 4, '梁雪宁', '女', '13800006666', 'liangxuening@example.com', '知识产权', '资深律师', 8, '梁雪宁律师服务多家科技公司，擅长商标、著作权、平台内容侵权及商业秘密保护。', '商标纠纷,著作权侵权,商业秘密', '周一至周五 09:00-17:00', 1)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  law_firm_id = VALUES(law_firm_id),
  name = VALUES(name),
  gender = VALUES(gender),
  phone = VALUES(phone),
  email = VALUES(email),
  category = VALUES(category),
  title = VALUES(title),
  experience_years = VALUES(experience_years),
  description = VALUES(description),
  good_at = VALUES(good_at),
  available_time_slots = VALUES(available_time_slots),
  status = VALUES(status),
  deleted = 0;

USE smart_law_appointment;

DELETE FROM lawyer_schedule WHERE lawyer_id BETWEEN 1 AND 6;

INSERT INTO lawyer_schedule (lawyer_id, weekday, start_time, end_time, enabled) VALUES
  (1, 1, '09:00:00', '17:00:00', 1), (1, 2, '09:00:00', '17:00:00', 1), (1, 3, '09:00:00', '17:00:00', 1), (1, 4, '09:00:00', '17:00:00', 1), (1, 5, '09:00:00', '17:00:00', 1),
  (2, 1, '09:00:00', '17:00:00', 1), (2, 2, '09:00:00', '17:00:00', 1), (2, 3, '09:00:00', '17:00:00', 1), (2, 4, '09:00:00', '17:00:00', 1), (2, 5, '09:00:00', '17:00:00', 1),
  (3, 1, '09:00:00', '17:00:00', 1), (3, 2, '09:00:00', '17:00:00', 1), (3, 3, '09:00:00', '17:00:00', 1), (3, 4, '09:00:00', '17:00:00', 1), (3, 5, '09:00:00', '17:00:00', 1),
  (4, 1, '09:00:00', '17:00:00', 1), (4, 2, '09:00:00', '17:00:00', 1), (4, 3, '09:00:00', '17:00:00', 1), (4, 4, '09:00:00', '17:00:00', 1), (4, 5, '09:00:00', '17:00:00', 1),
  (5, 1, '09:00:00', '17:00:00', 1), (5, 2, '09:00:00', '17:00:00', 1), (5, 3, '09:00:00', '17:00:00', 1), (5, 4, '09:00:00', '17:00:00', 1), (5, 5, '09:00:00', '17:00:00', 1),
  (6, 1, '09:00:00', '17:00:00', 1), (6, 2, '09:00:00', '17:00:00', 1), (6, 3, '09:00:00', '17:00:00', 1), (6, 4, '09:00:00', '17:00:00', 1), (6, 5, '09:00:00', '17:00:00', 1);

INSERT INTO legal_case (id, user_id, lawyer_id, title, start_time, end_time, status, deleted) VALUES
  (1, 2, 1, '互联网公司劳动仲裁开庭', '2026-07-06 10:00:00', '2026-07-06 12:00:00', 'ACTIVE', 0),
  (2, 2, 3, '婚姻家事调解会议', '2026-07-07 14:00:00', '2026-07-07 16:00:00', 'ACTIVE', 0),
  (3, 2, 6, '著作权侵权证据交换', '2026-07-08 09:00:00', '2026-07-08 11:00:00', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  lawyer_id = VALUES(lawyer_id),
  title = VALUES(title),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  status = VALUES(status),
  deleted = VALUES(deleted);

INSERT INTO appointment (id, user_id, lawyer_id, law_firm_id, law_firm_name, lawyer_name, appointment_time, duration_minutes, status, remark, deleted) VALUES
  (1, 2, 1, 1, '深圳前海湾律师事务所', '林启航', '2026-07-07 14:00:00', 60, '已预约', '用户通过智能法律助理预约法律咨询', 0),
  (2, 2, 5, 3, '哈尔滨松北明理律师事务所', '谭思远', '2026-07-09 15:00:00', 60, '已预约', '交通事故赔偿材料初步沟通', 0)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  lawyer_id = VALUES(lawyer_id),
  law_firm_id = VALUES(law_firm_id),
  law_firm_name = VALUES(law_firm_name),
  lawyer_name = VALUES(lawyer_name),
  appointment_time = VALUES(appointment_time),
  duration_minutes = VALUES(duration_minutes),
  status = VALUES(status),
  remark = VALUES(remark),
  deleted = VALUES(deleted);
