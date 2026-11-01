"""Adapt official fallen-tree selector chances to 1.20.1 without increasing tree attempts."""
import json
import zipfile
from backport_forest_resources import ROOT, RES, write, feature, placed


def main():
    with zipfile.ZipFile(ROOT/'refer/minecraft-26.3-client.jar') as new, zipfile.ZipFile(ROOT/'build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar') as old:
        def load(jar,path):return json.loads(jar.read(path))
        def old_feature(name):return load(old,f'data/minecraft/worldgen/configured_feature/{name}.json')
        for wood in ('oak','birch','super_birch','spruce','jungle'):
            source=load(new,f'data/minecraft/worldgen/feature/fallen_{wood}_tree.json')
            trunk=source['trunk_provider'];config={'trunk_provider':{'type':'minecraft:simple_state_provider','state':{
                'Name':trunk['id'],'Properties':trunk['properties']}},'log_length':{'type':'minecraft:uniform','value':{k:v for k,v in source['log_length'].items() if k!='type'}},
                'stump_vines':bool(source['stump_decorators'])}
            name=f'fallen_{wood}_tree';feature(name,'farmerstales:fallen_tree',config)
            sapling='birch' if wood=='super_birch' else wood
            write(RES/f'data/farmerstales/worldgen/placed_feature/{name}.json',{'feature':'farmerstales:'+name,'placement':[{
                'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:would_survive','state':{'Name':f'minecraft:{sapling}_sapling'}}}]})
        leaf_map={}
        # Clone, rather than edit the configured trees also used by saplings and other biomes.
        for configured,checked in (('oak','oak_checked'),('fancy_oak','fancy_oak_checked'),('birch','birch_checked'),('dark_oak','dark_oak_checked'),
                ('oak_bees_0002','oak_bees_0002'),('birch_bees_0002','birch_bees_0002'),('fancy_oak_bees_0002','fancy_oak_bees_0002')):
            target=configured+'_leaf_litter';data=old_feature(configured)
            data['config']['decorators'].append({'type':'farmerstales:leaf_litter'})
            write(RES/f'data/farmerstales/worldgen/configured_feature/{target}.json',data)
            placement=load(old,f'data/minecraft/worldgen/placed_feature/{checked}.json')
            placement['feature']='farmerstales:'+target
            write(RES/f'data/farmerstales/worldgen/placed_feature/{target}.json',placement)
            leaf_map['minecraft:'+checked]='farmerstales:'+target
        selectors=['birch_tall','dark_forest_vegetation','trees_birch_and_oak','trees_flower_forest','trees_jungle',
            'trees_old_growth_pine_taiga','trees_old_growth_spruce_taiga','trees_plains','trees_savanna','trees_sparse_jungle','trees_taiga','trees_windswept_hills']
        audit={}
        for name in selectors:
            modern='trees_birch_and_oak_leaf_litter' if name=='trees_birch_and_oak' else name
            source=load(new,f'data/minecraft/worldgen/feature/{modern}.json');data=old_feature(name)
            # Retain old standing tree feature identities and existing bee decorators; insert new fallen branches in their official order.
            existing=iter(data['config']['features']);branches=[]
            for branch in source['features']:
                ref=branch['feature']
                if isinstance(ref,str) and ref.startswith('minecraft:fallen_'):
                    branches.append({'chance':branch['chance'],'feature':ref.replace('minecraft:','farmerstales:',1)})
                else:branches.append(next(existing))
            assert next(existing,None) is None,name
            data['config']['features']=branches
            if name in ('trees_birch_and_oak','dark_forest_vegetation'):
                def leaves(value):
                    if isinstance(value,str):return leaf_map.get(value,value)
                    if isinstance(value,list):return [leaves(v) for v in value]
                    if isinstance(value,dict):return {k:leaves(v) for k,v in value.items()}
                    return value
                data=leaves(data)
            write(RES/f'data/minecraft/worldgen/configured_feature/{name}.json',data)
            audit[name]=[b for b in branches if isinstance(b['feature'],str) and 'fallen_' in b['feature']]
        # In 1.20.1 these three placed features directly reference a tree rather than a selector.
        for name,default in (('trees_birch','minecraft:birch_bees_0002'),('trees_badlands','farmerstales:oak_leaf_litter'),('trees_snowy','minecraft:spruce_checked')):
            source=load(new,f'data/minecraft/worldgen/feature/{name}.json')
            branches=[{'chance':b['chance'],'feature':b['feature'].replace('minecraft:fallen_','farmerstales:fallen_')} for b in source['features']]
            feature(name,'minecraft:random_selector',{'default':default,'features':branches})
            data=load(old,f'data/minecraft/worldgen/placed_feature/{name}.json');data['feature']='farmerstales:'+name
            write(RES/f'data/minecraft/worldgen/placed_feature/{name}.json',data);audit[name]=branches
        states=[{'data':{'Name':'farmerstales:leaf_litter','Properties':{'facing':direction,'flower_amount':str(amount)}},'weight':1}
            for amount in range(1,5) for direction in ('north','east','south','west')]
        feature('patch_forest_leaf_litter','minecraft:random_patch',{'tries':32,'xz_spread':7,'y_spread':3,'feature':{
            'feature':{'type':'minecraft:simple_block','config':{'to_place':{'type':'minecraft:weighted_state_provider','entries':states}}},
            'placement':[{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:all_of','predicates':[
                {'type':'minecraft:matching_blocks','blocks':['minecraft:air']},
                {'type':'minecraft:matching_blocks','blocks':['minecraft:grass_block'],'offset':[0,-1,0]}]}}]}})
        placed('patch_forest_leaf_litter','patch_forest_leaf_litter',count=2)
        # Forest leaf litter comes from its trees; dark forest additionally has this sparse ground patch in vanilla.
        write(RES/'data/farmerstales/forge/biome_modifier/add_dark_forest_leaf_litter.json',{'type':'forge:add_features',
            'biomes':['minecraft:dark_forest'],'features':['farmerstales:patch_forest_leaf_litter'],'step':'vegetal_decoration'})
        write(ROOT/'build/fallen-trees-selector-audit.json',audit)
        print('Generated 4 wood types plus long birch, 15 selector adaptations, 7 leaf-litter tree clones and dark-forest ground litter.')

if __name__=='__main__':main()
