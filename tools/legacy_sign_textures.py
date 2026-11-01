"""Lossless PNG decoding and legacy sign UV repacking (standard library only)."""
import struct,zlib,binascii

def read_png(data):
    pos=8;raw=b'';palette=None;alpha=None
    while pos<len(data):
        size=struct.unpack('>I',data[pos:pos+4])[0];kind=data[pos+4:pos+8];value=data[pos+8:pos+8+size];pos+=12+size
        if kind==b'IHDR':w,h,depth,color,*_=struct.unpack('>IIBBBBB',value)
        if kind==b'IDAT':raw+=value
        if kind==b'PLTE':palette=[tuple(value[i:i+3]) for i in range(0,len(value),3)]
        if kind==b'tRNS':alpha=value
    assert (depth==8 or color==3 and depth in (1,2,4)) and color in (2,3,6),(depth,color)
    channels={2:3,3:1,6:4}[color];stride=(w*depth+7)//8 if color==3 else w*channels;raw=zlib.decompress(raw);rows=[];previous=bytearray(stride);pos=0
    def paeth(a,b,c):
        p=a+b-c;ds=[abs(p-a),abs(p-b),abs(p-c)];return (a,b,c)[ds.index(min(ds))]
    for _ in range(h):
        filt=raw[pos];row=bytearray(raw[pos+1:pos+1+stride]);pos+=stride+1
        for i in range(stride):
            a=row[i-channels] if i>=channels else 0;b=previous[i];c=previous[i-channels] if i>=channels else 0
            row[i]=(row[i]+(0,a,b,(a+b)//2,paeth(a,b,c))[filt])&255
        pixels=[]
        for i in range(w) if color==3 else range(0,stride,channels):
            if color==3:
                n=(row[(i*depth)//8] >> (8-depth-(i*depth)%8)) & ((1<<depth)-1);pixels.append(palette[n]+((alpha[n] if alpha and n<len(alpha) else 255),))
            else:pixels.append(tuple(row[i:i+channels])+( (255,) if channels==3 else ()))
        rows.append(pixels);previous=row
    return rows

def png(rows):
    h=len(rows);w=len(rows[0])
    def chunk(k,d):return struct.pack('>I',len(d))+k+d+struct.pack('>I',binascii.crc32(k+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+bytes(c for p in row for c in p) for row in rows)))+chunk(b'IEND',b'')

def rectangles(u,v,w,h,d):
    return [(u+d,v,w,d),(u+d+w,v,w,d),(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),(u+2*d+w,v+d,w,h)]

def repack(old_data,new_reference,new_target,hanging=False):
    old=read_png(old_data);reference=read_png(new_reference);target=read_png(new_target)
    rects=rectangles(0,12,14,10,2)+rectangles(0,0,16,2,4)+[(0,6,3,6),(3,6,3,6),(6,6,3,6),(9,6,3,6),(14,6,12,6),(26,6,12,6)] if hanging else rectangles(0,0,24,12,2)+rectangles(0,14,2,14,2)
    output=[[(0,0,0,0)]*64 for _ in range(32)]
    for x,y,w,h in rects:
        if w==0 or h==0:continue
        pattern=[row[x:x+w] for row in old[y:y+h]]
        match=None
        for flip_y,flip_x in ((False,False),(True,False),(False,True),(True,True)):
            wanted=pattern[::-1] if flip_y else pattern
            if flip_x:wanted=[row[::-1] for row in wanted]
            for sy in range(len(reference)-h+1):
                for sx in range(len(reference[0])-w+1):
                    if all(reference[sy+i][sx:sx+w]==wanted[i] for i in range(h)):match=(sx,sy,flip_y,flip_x);break
                if match:break
            if match:break
        assert match is not None,('unmapped sign face',hanging,(x,y,w,h))
        sx,sy,flip_y,flip_x=match
        for dy in range(h):
            pixels=target[sy+(h-1-dy if flip_y else dy)][sx:sx+w]
            output[y+dy][x:x+w]=pixels[::-1] if flip_x else pixels
    return png(output)
