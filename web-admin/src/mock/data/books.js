import { faker } from '@faker-js/faker/locale/zh_CN'

// 图书分类数据(基于中图法)
export const categories = [
  { code: 'A', name: '马克思主义、列宁主义、毛泽东思想、邓小平理论' },
  { code: 'B', name: '哲学、宗教' },
  { code: 'C', name: '社会科学总论' },
  { code: 'D', name: '政治、法律' },
  { code: 'E', name: '军事' },
  { code: 'F', name: '经济' },
  { code: 'G', name: '文化、科学、教育、体育' },
  { code: 'H', name: '语言、文字' },
  { code: 'I', name: '文学' },
  { code: 'J', name: '艺术' },
  { code: 'K', name: '历史、地理' },
  { code: 'N', name: '自然科学总论' },
  { code: 'O', name: '数理科学和化学' },
  { code: 'P', name: '天文学、地球科学' },
  { code: 'Q', name: '生物科学' },
  { code: 'R', name: '医药、卫生' },
  { code: 'S', name: '农业科学' },
  { code: 'T', name: '工业技术' },
  { code: 'U', name: '交通运输' },
  { code: 'V', name: '航空、航天' },
  { code: 'X', name: '环境科学、安全科学' },
  { code: 'Z', name: '综合性图书' }
]

// 精选图书列表
const popularBooks = [
  {
    isbn: '9787020002207',
    title: '三体',
    author: '刘慈欣',
    publisher: '重庆出版社',
    category: 'I',
    categoryName: '文学',
    description: '文化大革命如火如荼进行的同时，军方探寻外星文明的绝秘计划"红岸工程"取得了突破性进展。但在按下发射键的那一刻，历史彻底改变了。',
    price: 23.0,
    pages: 302,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2768378.jpg'
  },
  {
    isbn: '9787020008735',
    title: '活着',
    author: '余华',
    publisher: '作家出版社',
    category: 'I',
    categoryName: '文学',
    description: '讲述了在大时代背景下,随着内战、三反五反,大跃进,文化大革命等社会变革,徐福贵的人生和家庭不断经受着苦难,到了最后所有亲人都先后离他而去,仅剩下年老的他和一头老牛相依为命。',
    price: 20.0,
    pages: 191,
    coverUrl: 'https://img2.doubanio.com/view/subject/l/public/s29053580.jpg'
  },
  {
    isbn: '9787020033683',
    title: '百年孤独',
    author: '加西亚·马尔克斯',
    publisher: '南海出版公司',
    category: 'I',
    categoryName: '文学',
    description: '魔幻现实主义文学的代表作,描写了布恩迪亚家族七代人的传奇故事,以及加勒比海沿岸小镇马孔多的百年兴衰,反映了拉丁美洲一个世纪以来风云变幻的历史。',
    price: 39.5,
    pages: 360,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s6384944.jpg'
  },
  {
    isbn: '9787115428028',
    title: 'Python编程:从入门到实践',
    author: 'Eric Matthes',
    publisher: '人民邮电出版社',
    category: 'TP',
    categoryName: '工业技术',
    description: 'Python入门经典教材,从基础知识到项目实战,循序渐进,适合零基础学习者。',
    price: 89.0,
    pages: 459,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28891064.jpg'
  },
  {
    isbn: '9787111421900',
    title: '算法导论(原书第3版)',
    author: 'Thomas H.Cormen等',
    publisher: '机械工业出版社',
    category: 'TP',
    categoryName: '工业技术',
    description: '算法领域的经典之作,全面系统地介绍了算法设计与分析的基础知识。',
    price: 128.0,
    pages: 780,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s26969502.jpg'
  },
  {
    isbn: '9787115404141',
    title: '深入理解计算机系统(原书第3版)',
    author: 'Randal E. Bryant',
    publisher: '机械工业出版社',
    category: 'TP',
    categoryName: '工业技术',
    description: '从程序员的视角深入浅出地讲解计算机系统的本质。',
    price: 139.0,
    pages: 736,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s29195878.jpg'
  },
  {
    isbn: '9787508647357',
    title: '人类简史',
    author: '尤瓦尔·赫拉利',
    publisher: '中信出版社',
    category: 'K',
    categoryName: '历史、地理',
    description: '从十万年前有生命迹象开始到21世纪资本、科技交织的人类发展史。',
    price: 68.0,
    pages: 440,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s27814883.jpg'
  },
  {
    isbn: '9787508660752',
    title: '未来简史',
    author: '尤瓦尔·赫拉利',
    publisher: '中信出版社',
    category: 'K',
    categoryName: '历史、地理',
    description: '进入21世纪后,曾经长期威胁人类生存、发展的瘟疫、饥荒和战争已经被攻克,智人面临着新的待办议题:永生不老、幸福快乐和成为具有"神性"的人类。',
    price: 68.0,
    pages: 396,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s29376248.jpg'
  },
  {
    isbn: '9787559614810',
    title: '原则',
    author: '瑞·达利欧',
    publisher: '中信出版社',
    category: 'F',
    categoryName: '经济',
    description: '美国对冲基金教父、桥水创始人瑞·达利欧的人生经验之作。',
    price: 98.0,
    pages: 550,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s29519833.jpg'
  },
  {
    isbn: '9787508638065',
    title: '自控力',
    author: '凯利·麦格尼格尔',
    publisher: '文化发展出版社',
    category: 'B',
    categoryName: '哲学、宗教',
    description: '斯坦福大学最受欢迎心理学课程,帮助你控制自己的注意力、情绪和欲望。',
    price: 39.8,
    pages: 271,
    coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s27001924.jpg'
  }
]

// 生成图书数据
export function generateBooks(count = 100) {
  const books = []

  // 添加精选图书
  popularBooks.forEach((book, index) => {
    const totalCopies = faker.number.int({ min: 3, max: 10 })
    const borrowedCopies = faker.number.int({ min: 0, max: totalCopies })

    books.push({
      id: index + 1,
      barcode: `BK${String(index + 1).padStart(8, '0')}`,
      ...book,
      publishDate: faker.date.past({ years: 10 }).toISOString().split('T')[0],
      callNumber: `${book.category}/${faker.string.alphanumeric(3).toUpperCase()}/${faker.number.int({ min: 1, max: 999 })}`,
      location: faker.helpers.arrayElement(['一楼书库', '二楼阅览室', '三楼参考室', '四楼特藏室']),
      totalCopies,
      availableCopies: totalCopies - borrowedCopies,
      borrowCount: faker.number.int({ min: 0, max: 500 }),
      status: borrowedCopies === totalCopies ? 'borrowed' : 'available',
      tags: faker.helpers.arrayElements(['推荐', '畅销', '经典', '新书', '必读'], faker.number.int({ min: 1, max: 3 })).join(','),
      createdAt: faker.date.past({ years: 2 }).toISOString(),
      updatedAt: faker.date.recent().toISOString()
    })
  })

  // 生成其他随机图书
  for (let i = popularBooks.length; i < count; i++) {
    const category = faker.helpers.arrayElement(categories)
    const totalCopies = faker.number.int({ min: 1, max: 8 })
    const borrowedCopies = faker.number.int({ min: 0, max: totalCopies })

    books.push({
      id: i + 1,
      barcode: `BK${String(i + 1).padStart(8, '0')}`,
      isbn: `978${faker.string.numeric(10)}`,
      title: faker.helpers.arrayElement([
        `${faker.word.adjective()}${faker.word.noun()}研究`,
        `现代${faker.word.noun()}导论`,
        `${faker.word.noun()}的艺术`,
        `${faker.word.adjective()}${faker.word.noun()}`,
        `${faker.word.noun()}与${faker.word.noun()}`,
      ]),
      author: faker.person.fullName(),
      publisher: faker.helpers.arrayElement([
        '人民出版社', '机械工业出版社', '清华大学出版社', '北京大学出版社',
        '电子工业出版社', '科学出版社', '高等教育出版社', '中信出版社'
      ]),
      publishDate: faker.date.past({ years: 10 }).toISOString().split('T')[0],
      category: category.code,
      categoryName: category.name,
      callNumber: `${category.code}/${faker.string.alphanumeric(3).toUpperCase()}/${faker.number.int({ min: 1, max: 999 })}`,
      price: faker.number.float({ min: 20, max: 150, fractionDigits: 1 }),
      pages: faker.number.int({ min: 100, max: 800 }),
      coverUrl: `https://picsum.photos/seed/${i}/200/300`,
      description: faker.lorem.paragraphs(2),
      keywords: faker.helpers.arrayElements(['技术', '管理', '创新', '实践', '理论', '应用'], 3).join(','),
      tags: faker.helpers.arrayElements(['推荐', '畅销', '经典', '新书'], faker.number.int({ min: 0, max: 2 })).join(','),
      location: faker.helpers.arrayElement(['一楼书库', '二楼阅览室', '三楼参考室', '四楼特藏室']),
      totalCopies,
      availableCopies: totalCopies - borrowedCopies,
      borrowCount: faker.number.int({ min: 0, max: 100 }),
      status: borrowedCopies === totalCopies ? 'borrowed' : (borrowedCopies > 0 ? 'available' : 'available'),
      createdAt: faker.date.past({ years: 2 }).toISOString(),
      updatedAt: faker.date.recent().toISOString()
    })
  }

  return books
}

// 生成图书统计数据
export function generateBookStats() {
  return {
    totalBooks: 10000,
    totalCopies: 25000,
    availableCopies: 18500,
    borrowedCopies: 6500,
    categoriesCount: categories.length,
    newBooksThisMonth: 120,
    popularBooksCount: 500,
    zeroCirculationCount: 800
  }
}