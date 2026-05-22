-- 商家表
CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(32) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    image_url VARCHAR(500),
    rating DECIMAL(2,1) DEFAULT 4.5,
    monthly_sales INT DEFAULT 0,
    delivery_time VARCHAR(20),
    delivery_fee BIGINT DEFAULT 0,
    min_order BIGINT DEFAULT 0,
    promo VARCHAR(100),
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜品表
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(32) UNIQUE NOT NULL,
    merchant_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price BIGINT NOT NULL,
    image_url VARCHAR(500),
    category VARCHAR(32),
    status VARCHAR(16) DEFAULT 'AVAILABLE',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 预置商家数据 ==========

INSERT INTO merchants (merchant_id, name, category, description, image_url, rating, monthly_sales, delivery_time, delivery_fee, min_order, promo) VALUES
-- 川湘菜
('M001', '张记麻辣烫', '川湘菜', '正宗四川麻辣烫，鲜香麻辣', 'https://picsum.photos/seed/m001/400/200', 4.8, 2300, '30分钟', 300, 1500, '满30减5'),
('M002', '湘味小厨', '川湘菜', '地道湖南家常菜', 'https://picsum.photos/seed/m002/400/200', 4.6, 1800, '35分钟', 200, 2000, '满50减8'),
-- 炸鸡汉堡
('M003', '炸鸡大师', '炸鸡汉堡', '金黄酥脆炸鸡，秘制酱料', 'https://picsum.photos/seed/m003/400/200', 4.7, 3500, '25分钟', 0, 1200, '满20减3'),
('M004', '汉堡工坊', '炸鸡汉堡', '手工现做汉堡，真材实料', 'https://picsum.photos/seed/m004/400/200', 4.5, 2100, '20分钟', 0, 1500, NULL),
-- 奶茶甜品
('M005', '茶颜悦色', '奶茶甜品', '鲜奶茶饮，每日现泡', 'https://picsum.photos/seed/m005/400/200', 4.9, 5200, '15分钟', 0, 800, '第二杯半价'),
('M006', '甜蜜时光', '奶茶甜品', '手工甜品，新鲜水果', 'https://picsum.photos/seed/m006/400/200', 4.6, 1600, '20分钟', 200, 1000, NULL),
-- 日韩料理
('M007', '和风日料', '日韩料理', '新鲜刺身，精致寿司', 'https://picsum.photos/seed/m007/400/200', 4.8, 1200, '40分钟', 500, 3000, '满60减10'),
('M008', '韩味烤肉', '日韩料理', '韩式烤肉，石锅拌饭', 'https://picsum.photos/seed/m008/400/200', 4.7, 1900, '35分钟', 300, 2500, '满40减6'),
-- 轻食沙拉
('M009', '绿野仙踪', '轻食沙拉', '新鲜蔬菜沙拉，健康轻食', 'https://picsum.photos/seed/m009/400/200', 4.5, 980, '20分钟', 0, 1500, NULL),
('M010', '轻食日记', '轻食沙拉', '低卡轻食，营养均衡', 'https://picsum.photos/seed/m010/400/200', 4.4, 750, '25分钟', 200, 1200, '新店满25减5'),
-- 烧烤
('M011', '炭火烧烤', '烧烤', '炭火现烤，串串飘香', 'https://picsum.photos/seed/m011/400/200', 4.7, 2800, '40分钟', 300, 2000, '满50减8'),
('M012', '烤串工厂', '烧烤', '各式烤串，深夜食堂', 'https://picsum.photos/seed/m012/400/200', 4.5, 2200, '35分钟', 200, 1800, NULL),
-- 粉面馆
('M013', '老北京炸酱面', '粉面馆', '正宗老北京风味', 'https://picsum.photos/seed/m013/400/200', 4.6, 1500, '25分钟', 0, 1000, '满20减3'),
('M014', '重庆小面', '粉面馆', '麻辣鲜香，地道重庆味', 'https://picsum.photos/seed/m014/400/200', 4.8, 2600, '20分钟', 0, 1200, NULL),
-- 小吃
('M015', '煎饼果子', '小吃', '现做煎饼，酥脆可口', 'https://picsum.photos/seed/m015/400/200', 4.5, 1800, '15分钟', 0, 500, NULL),
('M016', '水饺馆', '小吃', '手工水饺，皮薄馅大', 'https://picsum.photos/seed/m016/400/200', 4.7, 2100, '25分钟', 0, 1000, '满25减4');

-- ========== 预置菜品数据 ==========

-- M001 张记麻辣烫
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F001', 'M001', '经典麻辣烫', '荤素搭配，汤底浓郁', 2200, 'https://picsum.photos/seed/f001/200/200', '主食', 1),
('F002', 'M001', '肥牛麻辣烫', '新鲜肥牛，口感极佳', 2800, 'https://picsum.photos/seed/f002/200/200', '主食', 2),
('F003', 'M001', '海鲜麻辣烫', '虾仁鱿鱼，鲜香四溢', 3200, 'https://picsum.photos/seed/f003/200/200', '主食', 3),
('F004', 'M001', '凉拌木耳', '爽脆开胃', 800, 'https://picsum.photos/seed/f004/200/200', '小吃', 4);

-- M002 湘味小厨
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F005', 'M002', '小炒黄牛肉', '湖南经典家常菜', 3800, 'https://picsum.photos/seed/f005/200/200', '主食', 1),
('F006', 'M002', '剁椒鱼头', '鲜辣开胃，鱼肉嫩滑', 4200, 'https://picsum.photos/seed/f006/200/200', '主食', 2),
('F007', 'M002', '农家小炒肉', '下饭神器', 2800, 'https://picsum.photos/seed/f007/200/200', '主食', 3),
('F008', 'M002', '酸辣土豆丝', '酸辣爽口', 1200, 'https://picsum.photos/seed/f008/200/200', '小吃', 4);

-- M003 炸鸡大师
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F009', 'M003', '黄金炸鸡腿', '外酥里嫩，秘制调料', 1200, 'https://picsum.photos/seed/f009/200/200', '主食', 1),
('F010', 'M003', '香辣鸡翅', '香辣入味，越吃越想', 1500, 'https://picsum.photos/seed/f010/200/200', '主食', 2),
('F011', 'M003', '炸鸡套餐', '鸡腿+薯条+可乐', 2800, 'https://picsum.photos/seed/f011/200/200', '套餐', 3),
('F012', 'M003', '蜂蜜芥末鸡块', '甜辣交织', 1800, 'https://picsum.photos/seed/f012/200/200', '小吃', 4);

-- M004 汉堡工坊
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F013', 'M004', '经典牛肉堡', '双层牛肉，芝士满满', 2500, 'https://picsum.photos/seed/f013/200/200', '主食', 1),
('F014', 'M004', '香辣鸡腿堡', '酥脆鸡腿排，香辣酱', 2200, 'https://picsum.photos/seed/f014/200/200', '主食', 2),
('F015', 'M004', '薯条', '金黄酥脆', 800, 'https://picsum.photos/seed/f015/200/200', '小吃', 3),
('F016', 'M004', '洋葱圈', '香脆洋葱圈', 600, 'https://picsum.photos/seed/f016/200/200', '小吃', 4);

-- M005 茶颜悦色
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F017', 'M005', '幽兰拿铁', '招牌鲜奶茶', 1600, 'https://picsum.photos/seed/f017/200/200', '饮品', 1),
('F018', 'M005', '声声乌龙', '清香乌龙茶底', 1400, 'https://picsum.photos/seed/f018/200/200', '饮品', 2),
('F019', 'M005', '栀晓', '栀子花香奶茶', 1500, 'https://picsum.photos/seed/f019/200/200', '饮品', 3),
('F020', 'M005', '凤栖绿桂', '绿茶鲜奶茶', 1300, 'https://picsum.photos/seed/f020/200/200', '饮品', 4);

-- M006 甜蜜时光
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F021', 'M006', '芒果班戟', '新鲜芒果，奶油绵密', 1800, 'https://picsum.photos/seed/f021/200/200', '甜品', 1),
('F022', 'M006', '提拉米苏', '经典意式甜品', 2200, 'https://picsum.photos/seed/f022/200/200', '甜品', 2),
('F023', 'M006', '杨枝甘露', '芒果西柚椰汁', 1600, 'https://picsum.photos/seed/f023/200/200', '甜品', 3),
('F024', 'M006', '双皮奶', '水牛奶制作，丝滑浓郁', 1200, 'https://picsum.photos/seed/f024/200/200', '甜品', 4);

-- M007 和风日料
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F025', 'M007', '三文鱼刺身', '挪威进口，新鲜厚切', 4800, 'https://picsum.photos/seed/f025/200/200', '主食', 1),
('F026', 'M007', '鳗鱼饭', '蒲烧鳗鱼，酱汁浓郁', 3800, 'https://picsum.photos/seed/f026/200/200', '主食', 2),
('F027', 'M007', '寿司拼盘', '8种口味精选', 3200, 'https://picsum.photos/seed/f027/200/200', '主食', 3),
('F028', 'M007', '味增汤', '日式经典', 600, 'https://picsum.photos/seed/f028/200/200', '饮品', 4);

-- M008 韩味烤肉
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F029', 'M008', '韩式烤五花肉', '炭火烤制，配生菜蒜片', 3500, 'https://picsum.photos/seed/f029/200/200', '主食', 1),
('F030', 'M008', '石锅拌饭', '锅巴香脆，拌饭酱香', 2800, 'https://picsum.photos/seed/f030/200/200', '主食', 2),
('F031', 'M008', '部队火锅', '泡菜火腿，浓郁汤底', 4200, 'https://picsum.photos/seed/f031/200/200', '主食', 3),
('F032', 'M008', '韩式炸鸡', '甜辣酱汁', 2200, 'https://picsum.photos/seed/f032/200/200', '小吃', 4);

-- M009 绿野仙踪
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F033', 'M009', '凯撒沙拉', '经典凯撒酱，罗马生菜', 2200, 'https://picsum.photos/seed/f033/200/200', '主食', 1),
('F034', 'M009', '鸡胸肉沙拉', '低脂高蛋白', 2500, 'https://picsum.photos/seed/f034/200/200', '主食', 2),
('F035', 'M009', '牛油果沙拉', '新鲜牛油果，营养丰富', 2800, 'https://picsum.photos/seed/f035/200/200', '主食', 3),
('F036', 'M009', '鲜榨橙汁', '现榨无添加', 1200, 'https://picsum.photos/seed/f036/200/200', '饮品', 4);

-- M010 轻食日记
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F037', 'M010', '全麦三明治', '全麦面包，火腿生菜', 1800, 'https://picsum.photos/seed/f037/200/200', '主食', 1),
('F038', 'M010', '藜麦碗', '藜麦+蔬菜+鸡胸肉', 2600, 'https://picsum.photos/seed/f038/200/200', '主食', 2),
('F039', 'M010', '希腊酸奶碗', '酸奶+坚果+水果', 1600, 'https://picsum.photos/seed/f039/200/200', '甜品', 3),
('F040', 'M010', '果蔬汁', '胡萝卜苹果芹菜', 1000, 'https://picsum.photos/seed/f040/200/200', '饮品', 4);

-- M011 炭火烧烤
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F041', 'M011', '羊肉串', '新疆羊肉，炭火烤制', 500, 'https://picsum.photos/seed/f041/200/200', '烧烤', 1),
('F042', 'M011', '烤鸡翅', '蜜汁烤翅', 600, 'https://picsum.photos/seed/f042/200/200', '烧烤', 2),
('F043', 'M011', '烤鱿鱼', '鲜嫩弹牙', 800, 'https://picsum.photos/seed/f043/200/200', '烧烤', 3),
('F044', 'M011', '烤韭菜', '蒜蓉烤韭菜', 400, 'https://picsum.photos/seed/f044/200/200', '烧烤', 4),
('F045', 'M011', '烤茄子', '蒜蓉茄子', 600, 'https://picsum.photos/seed/f045/200/200', '烧烤', 5);

-- M012 烤串工厂
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F046', 'M012', '牛肉串', '精选牛肉，撒孜然', 600, 'https://picsum.photos/seed/f046/200/200', '烧烤', 1),
('F047', 'M012', '五花肉串', '肥瘦相间', 500, 'https://picsum.photos/seed/f047/200/200', '烧烤', 2),
('F048', 'M012', '烤年糕', '软糯Q弹', 400, 'https://picsum.photos/seed/f048/200/200', '烧烤', 3),
('F049', 'M012', '烤馒头片', '金黄酥脆', 300, 'https://picsum.photos/seed/f049/200/200', '烧烤', 4);

-- M013 老北京炸酱面
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F050', 'M013', '炸酱面', '手工面条，老北京炸酱', 1500, 'https://picsum.photos/seed/f050/200/200', '主食', 1),
('F051', 'M013', '打卤面', '木耳黄花菜卤', 1600, 'https://picsum.photos/seed/f051/200/200', '主食', 2),
('F052', 'M013', '凉面', '芝麻酱凉面', 1200, 'https://picsum.photos/seed/f052/200/200', '主食', 3),
('F053', 'M013', '拍黄瓜', '蒜泥拍黄瓜', 600, 'https://picsum.photos/seed/f053/200/200', '小吃', 4);

-- M014 重庆小面
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F054', 'M014', '重庆小面', '麻辣鲜香，正宗重庆味', 1400, 'https://picsum.photos/seed/f054/200/200', '主食', 1),
('F055', 'M014', '豌杂面', '豌豆杂酱，浓郁酱香', 1600, 'https://picsum.photos/seed/f055/200/200', '主食', 2),
('F056', 'M014', '牛肉面', '红烧牛肉，大块牛肉', 2200, 'https://picsum.photos/seed/f056/200/200', '主食', 3),
('F057', 'M014', '红油抄手', '皮薄馅嫩，红油飘香', 1200, 'https://picsum.photos/seed/f057/200/200', '小吃', 4);

-- M015 煎饼果子
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F058', 'M015', '经典煎饼果子', '鸡蛋+薄脆+甜面酱', 800, 'https://picsum.photos/seed/f058/200/200', '主食', 1),
('F059', 'M015', '培根煎饼', '培根鸡蛋煎饼', 1200, 'https://picsum.photos/seed/f059/200/200', '主食', 2),
('F060', 'M015', '火腿煎饼', '火腿肠+生菜', 1000, 'https://picsum.photos/seed/f060/200/200', '主食', 3),
('F061', 'M015', '豆浆', '现磨豆浆', 400, 'https://picsum.photos/seed/f061/200/200', '饮品', 4);

-- M016 水饺馆
INSERT INTO menu_items (item_id, merchant_id, name, description, price, image_url, category, sort_order) VALUES
('F062', 'M016', '猪肉白菜饺', '经典馅料', 1500, 'https://picsum.photos/seed/f062/200/200', '主食', 1),
('F063', 'M016', '韭菜鸡蛋饺', '鲜香韭菜', 1400, 'https://picsum.photos/seed/f063/200/200', '主食', 2),
('F064', 'M016', '三鲜水饺', '虾仁猪肉韭菜', 1800, 'https://picsum.photos/seed/f064/200/200', '主食', 3),
('F065', 'M016', '酸辣汤', '酸辣开胃', 600, 'https://picsum.photos/seed/f065/200/200', '饮品', 4);
