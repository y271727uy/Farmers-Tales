"""Validate the 1.20.1 resource graph, generated trim sprites and official text/sound hashes."""
import hashlib
import json
import zipfile
from pathlib import Path
from backport_eggs_creaking_resources import ROOT, RES, BLOCKS, ITEMS


def main():
    with zipfile.ZipFile(ROOT/'build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar') as old:
        vanilla=set(old.namelist())
        generated_sprites=set()
        def location(value): return value.split(':',1) if ':' in value else ('minecraft',value)
        def find(path):
            for root in (RES,ROOT/'src/generated/resources'):
                candidate=root/path
                if candidate.exists(): return candidate
            return None
        def read(path):
            pathfile=find(path)
            return json.loads(pathfile.read_text(encoding='utf-8')) if pathfile else json.loads(old.read(path))
        def exists(path): return find(path) is not None or path in vanilla
        for atlas in ('armor_trims','blocks'):
            sources=read(f'assets/minecraft/atlases/{atlas}.json')['sources']
            assert sources, f'Empty resin trim atlas {atlas}'
            for source in sources:
                assert source['permutations']=={'resin':'farmerstales:trims/color_palettes/resin'}
            vanilla_sources=json.loads(old.read(f'assets/minecraft/atlases/{atlas}.json'))['sources']
            for source in sources+vanilla_sources:
                if source['type'].removeprefix('minecraft:')!='paletted_permutations':continue
                for tex in source['textures']:
                    namespace,path=location(tex)
                    assert exists(f'assets/{namespace}/textures/{path}.png'), tex
                    for suffix in source['permutations']: generated_sprites.add(f'{namespace}:{path}_{suffix}')
        visited=set()
        def model(ref):
            namespace,path=location(ref)
            if path.startswith('builtin/'): return
            resource=f'assets/{namespace}/models/{path}.json'
            assert exists(resource),ref
            if resource in visited:return
            visited.add(resource);data=read(resource)
            if 'parent' in data:model(data['parent'])
            for tex in data.get('textures',{}).values():
                if tex.startswith('#'):continue
                ns,rel=location(tex)
                assert f'{ns}:{rel}' in generated_sprites or exists(f'assets/{ns}/textures/{rel}.png'),tex
            for override in data.get('overrides',[]):model(override['model'])
        def state(data):
            if isinstance(data,list):
                for entry in data:state(entry)
            elif isinstance(data,dict):
                for key,value in data.items():
                    if key=='model':model(value)
                    else:state(value)
        for name in BLOCKS:
            state(read(f'assets/farmerstales/blockstates/{name}.json'))
            assert find(f'data/farmerstales/loot_tables/blocks/{name}.json'),name
        for name in BLOCKS+ITEMS:model(f'farmerstales:item/{name}')
        armor=list((RES/'assets/farmerstales/models/item').glob('*_resin_trim.json'))
        assert len(armor)==25,len(armor)
        for path in armor:
            item=path.stem.removesuffix('_resin_trim')
            data=read(f'assets/minecraft/models/item/{item}.json')
            assert data['overrides'][-1]=={'predicate':{'trim_type':1.1},'model':f'farmerstales:item/{item}_resin_trim'}
            model(f'minecraft:item/{item}')
        for kind in ('blocks','items'):
            for suffix,block in (('stairs','stairs'),('slabs','slab'),('walls','wall')):
                values=read(f'data/minecraft/tags/{kind}/{suffix}.json')['values']
                assert 'farmerstales:resin_brick_'+block in values
                assert 'farmerstales:resin_brick_stair' not in values
        snapshot=json.loads((ROOT/'tools/data/minecraft-26.3-official-zh.json').read_text(encoding='utf-8'))
        translations=0
        for locale,asset in snapshot['locales'].items():
            current=read(f'assets/farmerstales/lang/{locale}.json')
            for key,value in asset['translations'].items():
                target=key.replace('.minecraft.','.farmerstales.',1) if '.minecraft.' in key else key.replace('subtitles.','subtitles.farmerstales.',1)
                assert current[target]==value,(locale,key)
                translations+=1
        index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects']
        sounds=read('assets/farmerstales/sounds.json');verified=set()
        for name,event in sounds.items():
            if not name.startswith(('entity.creaking.','block.creaking_heart.','block.resin.','block.resin_bricks.')):continue
            for sound in event['sounds']:
                ref=sound if isinstance(sound,str) else sound['name'];ns,path=location(ref)
                data=(RES/f'assets/{ns}/sounds/{path}.ogg').read_bytes()
                assert hashlib.sha1(data).hexdigest()==index[f'minecraft/sounds/{path}.ogg']['hash'],path
                verified.add(path)
        report={'models_checked':len(visited),'armor_models':len(armor),'generated_trim_sprites':len(generated_sprites),'official_translations':translations,'verified_sounds':len(verified)}
        output=ROOT/'build/eggs-creaking-resource-audit.json';output.write_text(json.dumps(report,indent=2)+'\n')
        print(json.dumps(report))

if __name__=='__main__':main()
