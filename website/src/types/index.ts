export interface ReleaseInfo {
  version: string;
  versionCode: number;
  releaseDate: string;
  fileSize: string;
  fileSizeBytes: number;
  apkFileName: string;
  downloadUrl: string;
  sha256: string;
  md5: string;
  minAndroid: string;
  targetAndroid: string;
  architectures: string[];
  packageName: string;
}

export interface FeatureItem {
  id: string;
  title: string;
  description: string;
  icon: string;
  badge?: string;
  highlights: string[];
}

export interface FaqItem {
  question: string;
  answer: string;
  category: 'download' | 'installation' | 'playback' | 'privacy';
}

export interface ChangelogItem {
  version: string;
  date: string;
  tag: 'Latest' | 'Stable' | 'Baseline';
  added: string[];
  improved: string[];
  fixed: string[];
}

export interface SpecRequirement {
  feature: string;
  minimum: string;
  recommended: string;
  note?: string;
}
