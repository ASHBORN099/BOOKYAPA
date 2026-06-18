import JSZip from "jszip";

export interface EpubChapter {
  title: string;
  offset: number;
}

export interface EpubResult {
  html: string;
  chapters: EpubChapter[];
  textContent: string;
}

const IMAGE_EXTS = /\.(png|jpg|jpeg|gif|svg|webp)$/i;
const MIME_TYPES: Record<string, string> = {
  png: "image/png",
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  gif: "image/gif",
  svg: "image/svg+xml",
  webp: "image/webp",
};

function normalizePath(path: string): string {
  const parts = path.split("/");
  const out: string[] = [];
  for (const p of parts) {
    if (p === "." || p === "") continue;
    if (p === "..") { if (out.length > 0) out.pop(); }
    else out.push(p);
  }
  return out.join("/");
}

function replaceImageSrcs(
  html: string,
  xhtmlZipPath: string,
  imageMap: Record<string, string>,
): string {
  const dir = xhtmlZipPath.includes("/")
    ? xhtmlZipPath.substring(0, xhtmlZipPath.lastIndexOf("/") + 1)
    : "";
  return html.replace(/src\s*=\s*["']([^"']+)["']/gi, (match, src: string) => {
    if (src.startsWith("data:") || src.startsWith("http://") || src.startsWith("https://") || src.startsWith("blob:")) {
      return match;
    }
    const combined = dir + src;
    const normalized = normalizePath(combined);
    const dataUri = imageMap[normalized] || imageMap[src];
    return dataUri ? `src="${dataUri}"` : match;
  });
}

function extractTextContent(xhtml: string): string {
  return xhtml
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#(\d+);/g, (_: string, d: string) =>
      String.fromCharCode(parseInt(d, 10)),
    )
    .replace(/&#x([0-9a-fA-F]+);/g, (_: string, h: string) =>
      String.fromCharCode(parseInt(h, 16)),
    )
    .trim();
}

function getBodyContent(xhtml: string): string {
  const bodyMatch = xhtml.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
  if (bodyMatch) return bodyMatch[1].trim();
  const htmlMatch = xhtml.match(/<html[^>]*>([\s\S]*?)<\/html>/i);
  if (htmlMatch) return htmlMatch[1].trim();
  return xhtml;
}

export async function parseEpub(epubUrl: string): Promise<EpubResult> {
  let response = await fetch(epubUrl);
  if (!response.ok)
    throw new Error(`Failed to fetch EPUB: HTTP ${response.status}`);

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("text/html")) {
    const html = await response.text();
    const linkMatch = html.match(/href="([^"]+\.epub)"/i);
    if (!linkMatch)
      throw new Error("EPUB URL returned HTML, no .epub link found");
    const resolvedUrl = new URL(linkMatch[1], epubUrl).toString();
    response = await fetch(resolvedUrl);
    if (!response.ok)
      throw new Error(`Failed to fetch resolved EPUB: HTTP ${response.status}`);
  }

  const arrayBuffer = await response.arrayBuffer();
  const zip = await JSZip.loadAsync(arrayBuffer);

  const imageMap: Record<string, string> = {};
  const imageFiles: Array<{ path: string; file: JSZip.JSZipObject }> = [];
  zip.forEach((relPath, file) => {
    if (!file.dir && IMAGE_EXTS.test(relPath)) {
      imageFiles.push({ path: relPath, file });
    }
  });
  for (const { path, file } of imageFiles) {
    const ext = path.split(".").pop()?.toLowerCase() || "png";
    const mime = MIME_TYPES[ext] || "image/png";
    imageMap[normalizePath(path)] = `data:${mime};base64,${await file.async("base64")}`;
  }

  const containerXml = zip.file("META-INF/container.xml");
  if (!containerXml) throw new Error("EPUB missing META-INF/container.xml");

  const containerStr = await containerXml.async("text");
  const opfMatch = containerStr.match(/full-path="([^"]+)"/);
  if (!opfMatch)
    throw new Error("Cannot find content.opf path in container.xml");

  const opfPath = opfMatch[1];
  const opfDir = opfPath.substring(0, opfPath.lastIndexOf("/") + 1);

  const opfFile = zip.file(opfPath);
  if (!opfFile) throw new Error(`EPUB missing ${opfPath}`);

  const opfStr = await opfFile.async("text");

  const manifestItems: Record<string, string> = {};
  const itemRegex = /<item\s[^>]*>/gi;
  let itemMatch: RegExpExecArray | null;
  while ((itemMatch = itemRegex.exec(opfStr)) !== null) {
    const tag = itemMatch[0];
    const id = tag.match(/id="([^"]+)"/)?.[1];
    const href = tag.match(/href="([^"]+)"/)?.[1];
    const mediaType = tag.match(/media-type="([^"]+)"/)?.[1];
    if (id && href && mediaType?.includes("xhtml")) {
      manifestItems[id] = opfDir + href;
    }
  }

  const spineRefs: string[] = [];
  const spineRegex = /<itemref\s[^>]*\/?>/gi;
  let spineMatch: RegExpExecArray | null;
  while ((spineMatch = spineRegex.exec(opfStr)) !== null) {
    const ref = spineMatch[0].match(/idref="([^"]+)"/)?.[1];
    if (ref) spineRefs.push(ref);
  }

  const parts: string[] = [];
  const chapters: EpubChapter[] = [];
  let textOffset = 0;

  for (const ref of spineRefs) {
    const href = manifestItems[ref];
    if (!href) continue;

    const file = zip.file(href);
    if (!file) continue;

    const xhtml = await file.async("text");
    const bodyContent = getBodyContent(xhtml);
    const withImages = replaceImageSrcs(bodyContent, href, imageMap);
    parts.push(withImages);

    const titleMatch = xhtml.match(/<h[1-6][^>]*>([^<]+)<\/h[1-6]>/i);
    const chapterTitle = titleMatch
      ? titleMatch[1].trim()
      : `Chapter ${chapters.length + 1}`;

    const textLen = extractTextContent(bodyContent).length;
    chapters.push({ title: chapterTitle, offset: textOffset });
    textOffset += textLen;
  }

  const html = parts.join("\n");
  const textContent = extractTextContent(html);

  return { html, chapters, textContent };
}
