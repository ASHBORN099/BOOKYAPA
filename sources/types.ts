// sources/types.ts

export interface StreamableBook {
  id: string;          
  title: string;       
  author: string;      
  coverUrl: string;    
  epubUrl: string;     
  htmlUrl: string;     
  sourceName: string;  
}

// 🔖 Kept for our upcoming Local Storage bookshelf features
export interface ReaderParams {
  id: string;
  title: string;
  bookUrl: string;
  coverUrl: string;
  epubUrl?: string;
}

// 📑 Restoring this so GutenbergSource stops complaining!
export interface BookChapter {
  title: string;       
  contentUrl: string;  
}