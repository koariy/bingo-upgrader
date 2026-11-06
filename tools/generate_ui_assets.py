from PIL import Image,ImageDraw
from pathlib import Path
import math,json
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/bingo_upgrader';(p/'textures/gui').mkdir(parents=True,exist_ok=True)
def panel(name,color,edge):
 im=Image.new('RGBA',(32,32));mask=Image.new('L',(32,32));ImageDraw.Draw(mask).rounded_rectangle((0,0,31,31),5,fill=255)
 for y in range(32):
  t=y/31;rgb=tuple(max(0,int(c*(1.13-.24*t))) for c in color[:3])+(color[3],);ImageDraw.Draw(im).line((0,y,31,y),fill=rgb)
 im.putalpha(Image.eval(mask,lambda a:int(a*color[3]/255)));d=ImageDraw.Draw(im);d.rounded_rectangle((0,0,31,31),5,outline=edge,width=1);d.line((6,1,25,1),fill=(255,211,138,60));im.save(p/'textures/gui'/name)
panel('panel.png',(20,29,44,248),(86,104,130,255));panel('button.png',(33,43,59,255),(99,116,137,255));panel('button_hover.png',(63,48,29,255),(255,185,73,255));panel('slot.png',(12,20,31,255),(75,93,116,255))
im=Image.new('RGBA',(48,32));d=ImageDraw.Draw(im)
for k in range(-2,7):
 for j in range(-2,5):
  cx=k*12;cy=j*16+(k%2)*8
  d.line([(cx-8,cy),(cx-4,cy-8),(cx+4,cy-8),(cx+8,cy),(cx+4,cy+8),(cx-4,cy+8),(cx-8,cy)],fill=(94,116,143,50),width=1)
im.save(p/'textures/gui/honeycomb.png')
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);d.line((3,8,6,12,13,3),fill=(90,229,141,255),width=3);im.save(p/'textures/gui/check.png')
im=Image.new('RGBA',(16,20));d=ImageDraw.Draw(im);d.polygon([(8,0),(14,14),(9,11),(9,19),(6,19),(6,11),(1,14)],fill=(255,204,94),outline=(77,54,23));im.save(p/'textures/gui/pointer.png')
# Cached ring atlas: frame = degrees of the winning sector, top origin, clockwise.
atlas=Image.new('RGBA',(2432,2432))
for degrees in range(361):
 im=Image.new('RGBA',(256,256));d=ImageDraw.Draw(im)
 for i in range(60):
  a=i*math.tau/60-math.pi/2;c=math.cos(a);s=math.sin(a)
  d.line((128+c*116,128+s*116,128+c*123,128+s*123),fill=(79,96,120,255),width=2)
 d.ellipse((24,24,232,232),fill=(46,61,79,255));d.ellipse((50,50,206,206),fill=(0,0,0,0))
 if degrees:
  d.pieslice((24,24,232,232),-90,-90+degrees,fill=(255,182,65,255));d.ellipse((50,50,206,206),fill=(0,0,0,0))
 im=im.resize((128,128),Image.Resampling.LANCZOS);atlas.paste(im,((degrees%19)*128,(degrees//19)*128))
atlas.save(p/'textures/gui/wheel_atlas.png')
# Vanilla sound-event references: complete usable resources with no copied game sound files.
sounds={'click':{'sounds':[{'name':'minecraft:ui.button.click','type':'event','volume':.25}]},'tick':{'sounds':[{'name':'minecraft:block.note_block.hat','type':'event','volume':.25}]},'success':{'sounds':[{'name':'minecraft:entity.experience_orb.pickup','type':'event'}]},'failure':{'sounds':[{'name':'minecraft:block.note_block.bass','type':'event','pitch':.7}]}}
(p/'sounds.json').write_text(json.dumps(sounds,indent=2))
print('UI textures and sound aliases generated')

# Radial glow, small block face and mod icon use the same palette.
im=Image.new('RGBA',(128,128));pix=im.load()
for y in range(128):
 for x in range(128):
  distance=((x-63.5)**2+(y-63.5)**2)**.5/64
  pix[x,y]=(255,255,255,int(max(0,1-distance)**2*200))
im.save(p/'textures/gui/glow.png')
(p/'textures/block').mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(16,16),(19,28,43,255));d=ImageDraw.Draw(im);d.rectangle((0,0,15,15),outline=(88,105,126));d.ellipse((3,3,12,12),outline=(255,183,65),width=2);d.rectangle((7,0,8,4),fill=(255,221,131));im.save(p/'textures/block/upgrader.png')
icon=Image.new('RGBA',(128,128),(17,24,39,255));d=ImageDraw.Draw(icon);d.rounded_rectangle((3,3,124,124),16,outline=(103,122,148),width=2);d.arc((22,22,106,106),-90,190,fill=(255,183,65),width=12);d.polygon(((64,12),(58,29),(70,29)),fill=(255,221,131));icon.save(p/'icon.png')
