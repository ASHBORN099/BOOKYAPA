const THEME_COLORS: Record<string, { bg: string; text: string }> = {
  white: { bg: "#ffffff", text: "#1a1a1a" },
  sepia: { bg: "#f5e6c8", text: "#3b2b1a" },
  dark: { bg: "#121212", text: "#e0e0e0" },
};

export interface WebViewHtmlOptions {
  content: string;
  textContent: string;
  isEpub: boolean;
  fontSize: number;
  lineHeight: number;
  themeBg: "white" | "sepia" | "dark";
  brightness: number;
  pageMode: boolean;
  totalTextLength: number;
  savedOffset: number;
}

export function buildWebViewHtml(options: WebViewHtmlOptions): string {
  const {
    content,
    textContent,
    isEpub,
    fontSize,
    lineHeight,
    themeBg,
    brightness,
    pageMode,
    totalTextLength,
    savedOffset,
  } = options;

  const colors = THEME_COLORS[themeBg] || THEME_COLORS.dark;
  const displayContent = isEpub
    ? content.replace(/<script[\s\S]*?<\/script>/gi, '').replace(/<style[\s\S]*?<\/style>/gi, '')
    : escapeHtml(content);

  if (!pageMode) {
    return buildScrollHtml(displayContent, { fontSize, lineHeight, colors, brightness, totalTextLength, savedOffset });
  }

  return buildFlipHtml(displayContent, { textContent: textContent || '', fontSize, lineHeight, colors, brightness, totalTextLength, savedOffset });
}

function buildScrollHtml(
  content: string,
  opts: {
    fontSize: number;
    lineHeight: number;
    colors: { bg: string; text: string };
    brightness: number;
    totalTextLength: number;
    savedOffset: number;
  },
): string {
  const { fontSize, lineHeight, colors, brightness, totalTextLength: ttl, savedOffset: sv } = opts;
  return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
:root{--font-size:${fontSize}px;--line-height:${lineHeight};--text-color:${colors.text};--bg-color:${colors.bg}}
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100%;overflow:hidden;background:var(--bg-color);color:var(--text-color);font-family:Georgia,'Times New Roman',serif}
body{padding:10px 24px}
#container{width:100%;height:100%;overflow-x:hidden;overflow-y:scroll}
#content{white-space:pre-wrap;word-wrap:break-word;font-size:var(--font-size);line-height:var(--line-height);color:var(--text-color);text-align:justify}
#content p{margin:0 0 0.5em 0;text-indent:1.5em}
#content p:first-of-type{text-indent:0}
img{max-width:100%;height:auto}
#overlay{position:fixed;inset:0;background:rgba(0,0,0,${1 - brightness});pointer-events:none;z-index:999}
</style>
</head>
<body>
<div id="container"><div id="content">${content}</div></div>
<div id="overlay"></div>
<script>
(function(){
function p(msg){var s=JSON.stringify(msg);if(window.ReactNativeWebView)window.ReactNativeWebView.postMessage(s);else if(window.parent&&window.parent!==window)window.parent.postMessage(s,'*')}
var c=document.getElementById('container'),co=document.getElementById('content'),ov=document.getElementById('overlay'),ttl=${ttl},sv=${sv};
var cp=0,st=0,startX=0;
if(sv>0) setTimeout(function(){var r=Math.min(1,sv/ttl);c.scrollTop=r*(c.scrollHeight-c.clientHeight);},100);
c.addEventListener('scroll',function(){
var mh=c.scrollHeight-c.clientHeight,ratio=mh>0?c.scrollTop/mh:0,off=Math.floor(ratio*ttl);
p({type:'progress',offset:isNaN(off)?0:off,page:0,totalPages:1});
});
c.addEventListener('touchstart',function(e){st=Date.now();startX=e.touches[0].clientX},{passive:true});
c.addEventListener('touchend',function(e){
var dt=Date.now()-st,dx=Math.abs(e.changedTouches[0].clientX-startX);
if(dt<300&&dx<20){var r=c.getBoundingClientRect(),x=e.changedTouches[0].clientX-r.left,w=r.width;if(x>w/3&&x<w*2/3)p({type:'toggleMenu'})}
},{passive:true});
window.addEventListener('message',function(e){
try{var d=JSON.parse(e.data);switch(d.type){
case'goToOffset':
var r=Math.min(1,d.offset/ttl);c.scrollTop=r*(c.scrollHeight-c.clientHeight);
p({type:'progress',offset:d.offset,page:0,totalPages:1});
break;
case'toggleMenu':p({type:'toggleMenu'});break;
case'goPrevPage':c.scrollTop=Math.max(0,c.scrollTop-c.clientHeight*0.9);break;
case'goNextPage':c.scrollTop=Math.min(c.scrollHeight-c.clientHeight,c.scrollTop+c.clientHeight*0.9);break;
case'updateSettings':
if(d.fontSize) document.documentElement.style.setProperty('--font-size',d.fontSize+'px');
if(d.lineHeight) document.documentElement.style.setProperty('--line-height',d.lineHeight);
if(d.themeColors){document.documentElement.style.setProperty('--text-color',d.themeColors.text);document.documentElement.style.setProperty('--bg-color',d.themeColors.bg);document.body.style.background=d.themeColors.bg}
if(d.brightness!==undefined) ov.style.background='rgba(0,0,0,'+(1-d.brightness)+')';
break;
}}catch(e){}}
);
})();
</script>
</body>
</html>`;
}

function buildFlipHtml(
  content: string,
  opts: {
    textContent: string;
    fontSize: number;
    lineHeight: number;
    colors: { bg: string; text: string };
    brightness: number;
    totalTextLength: number;
    savedOffset: number;
  },
): string {
  const { fontSize, lineHeight, colors, brightness, savedOffset: sv } = opts;
  const text = opts.textContent || '';
  return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100vh;overflow:hidden}
body{padding:10px 24px;background:${colors.bg};color:${colors.text};font-family:Georgia,'Times New Roman',serif}
#container{width:100%;height:calc(100vh - 20px);overflow:hidden;position:relative}
#measurer{position:fixed;left:24px;top:10px;visibility:hidden;z-index:-1;white-space:pre-wrap;word-wrap:break-word;font-size:${fontSize}px;line-height:${lineHeight};width:calc(100vw - 48px)}
#content{position:absolute;left:0;top:0;width:100%;height:100%;overflow:hidden;white-space:pre-wrap;word-wrap:break-word;font-size:${fontSize}px;line-height:${lineHeight};color:${colors.text};text-align:justify;transition:transform .2s ease,opacity .2s ease}
#overlay{position:fixed;inset:0;background:rgba(0,0,0,${1 - brightness});pointer-events:none;z-index:999}
</style>
</head>
<body>
<div id="container">
<div id="measurer"></div>
<div id="content"></div>
</div>
<div id="overlay"></div>
<script>
(function(){
var text = ${JSON.stringify(text)};
var fontSize=${fontSize},lineHeight=${lineHeight};
var sv=${sv};
var measurer=document.getElementById('measurer'),content=document.getElementById('content'),ov=document.getElementById('overlay'),container=document.getElementById('container');
var charsPerPage=0,pages=[],currentPage=0,startX=0,flipDir=0,animating=false;

function p(msg){var s=JSON.stringify(msg);if(window.ReactNativeWebView)window.ReactNativeWebView.postMessage(s);else if(window.parent&&window.parent!==window)window.parent.postMessage(s,'*')}

function measure(){
var th=window.innerHeight-20;
measurer.style.width=(window.innerWidth-48)+'px';
measurer.style.fontSize=fontSize+'px';
measurer.style.lineHeight=lineHeight;
measurer.style.visibility='visible';
var low=1,high=text.length;
while(low<high){var mid=Math.ceil((low+high)/2);measurer.textContent=text.substring(0,mid);if(measurer.scrollHeight>th)high=mid-1;else low=mid}
charsPerPage=Math.max(1,low);
measurer.style.visibility='hidden';
}

function computePages(){
pages=[];var pos=0,len=text.length,th=window.innerHeight-20;
measurer.style.visibility='visible';
while(pos<len){
var end=Math.min(pos+charsPerPage,len);
if(end<len){
measurer.textContent=text.substring(pos,end);
if(measurer.scrollHeight>th){
var lo=pos+1,hi=end;
while(lo<hi){var mid=Math.ceil((lo+hi)/2);measurer.textContent=text.substring(pos,mid);if(measurer.scrollHeight>th)hi=mid-1;else lo=mid}
end=lo;
}
var bp=end;while(bp>pos&&text[bp]!==' '&&text[bp]!=='\\n')bp--;if(bp>pos)end=bp;
}
pages.push({start:pos,end:end});pos=end;
}
measurer.style.visibility='hidden';
}

function findPage(offset){
var lo=0,hi=pages.length-1;
while(lo<hi){var mid=Math.ceil((lo+hi)/2);if(pages[mid].start>offset)hi=mid-1;else lo=mid}
return lo
}

function showPage(page){
page=Math.max(0,Math.min(page,pages.length-1));
if(page===currentPage)return;
var b=pages[page];
content.textContent=text.substring(b.start,b.end);
currentPage=page;
p({type:'progress',offset:b.start,page:currentPage,totalPages:pages.length})
}

document.addEventListener('touchstart',function(e){
e.preventDefault();if(animating)return;
startX=e.touches[0].clientX;
var x=e.touches[0].clientX,w=window.innerWidth;
flipDir=x<w/3?-1:x>w*2/3?1:0;
},{passive:false});

document.addEventListener('touchmove',function(e){e.preventDefault()},{passive:false});

document.addEventListener('touchend',function(e){
e.preventDefault();if(animating)return;
var dx=e.changedTouches[0].clientX-startX;
if(Math.abs(dx)>30){var np=dx>0?currentPage-1:currentPage+1;if(np>=0&&np<pages.length)showPage(np);return}
if(flipDir===-1&&currentPage>0)showPage(currentPage-1);
else if(flipDir===1&&currentPage<pages.length-1)showPage(currentPage+1);
else if(flipDir===0)p({type:'toggleMenu'})
},{passive:false});

window.addEventListener('message',function(e){
try{var d=JSON.parse(e.data);switch(d.type){
case'goToOffset':showPage(findPage(d.offset));break;
case'goPrevPage':if(currentPage>0)showPage(currentPage-1);break;
case'goNextPage':if(currentPage<pages.length-1)showPage(currentPage+1);break;
case'toggleMenu':p({type:'toggleMenu'});break;
case'updateSettings':
if(d.fontSize){fontSize=d.fontSize;document.documentElement.style.setProperty('--font-size',d.fontSize+'px');content.style.fontSize=d.fontSize+'px'}
if(d.lineHeight){lineHeight=d.lineHeight;document.documentElement.style.setProperty('--line-height',d.lineHeight);content.style.lineHeight=d.lineHeight}
if(d.themeColors){document.documentElement.style.setProperty('--text-color',d.themeColors.text);document.documentElement.style.setProperty('--bg-color',d.themeColors.bg);document.body.style.background=d.themeColors.bg;content.style.color=d.themeColors.text}
if(d.brightness!==undefined)ov.style.background='rgba(0,0,0,'+(1-d.brightness)+')';
container.style.height=(window.innerHeight-20)+'px';measure();computePages();showPage(Math.min(currentPage,pages.length-1));
break
}}catch(e){}}
);

container.style.height=(window.innerHeight-20)+'px';
measure();computePages();
if(sv>0)showPage(findPage(sv));
else showPage(0);
})();
</script>
</body>
</html>`;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
