package Terrain;

import LibrarySupport.Maths;
import LibrarySupport.Vector2f;
import LibrarySupport.Vector3f;
import Models.RawModel;
import RenderEngine.Loader;
import Textures.ModelTexture;
import Textures.TerrainTexture;
import Textures.TerrainTexturePack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/*
    Terrain Class

*/
public class Terrain {

//    map size
    private static final float SIZE = 1600;
    private static final float MAX_HEIGHT = 40;
    private static final float MAX_PIXEL_COLOUR = 256 * 256 * 256;

//    table of terrain heights
    private float[][] heights;


    private float x;
    private float z;
    private RawModel model;
    private TerrainTexturePack texturePack;
    private TerrainTexture blendMap; //handle information about shape of world map

    public Terrain(int gridX, int gridZ, Loader loader,
                   TerrainTexturePack texturePack, TerrainTexture blendMap, String heightMap){
        this.texturePack = texturePack;
        this.blendMap = blendMap;
        this.x = gridX * SIZE;
        this.z = gridZ * SIZE;
        this.model = generateTerrain(loader, heightMap);
    }

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    public RawModel getModel() {
        return model;
    }

    public TerrainTexturePack getTexturePack() {
        return texturePack;
    }

    public TerrainTexture getBlendMap() {
        return blendMap;
    }

    public float getHeightOfTerrain(float worldX, float worldZ){
        float terrainX = worldX - this.x;
        float terrainZ = worldZ - this.z;
        float gridSquareSize = SIZE/ (float) (heights.length -1);
        int gridX = (int) Math.floor(terrainX / gridSquareSize);
        int gridZ = (int) Math.floor(terrainZ / gridSquareSize);
        if(gridX >= heights.length - 1 || gridZ >= heights.length - 1 || gridX < 0 || gridZ < 0){
            return 0;
        }
        float xCoord = (terrainX % gridSquareSize)/gridSquareSize;
        float zCoord = (terrainZ % gridSquareSize)/gridSquareSize;
        float answer;
        if(xCoord <= (1-zCoord)){
            answer = Maths.bayrionicCentric(new Vector3f(0, heights[gridX][gridZ], 0),
                    new Vector3f(1, heights[gridX + 1][gridZ], 0),
                    new Vector3f(0, heights[gridX][gridZ + 1],1),
                    new Vector2f(xCoord, zCoord));

        }else{
            answer = Maths.bayrionicCentric(new Vector3f(1, heights[gridX + 1][gridZ], 0),
                    new Vector3f(1, heights[gridX + 1][gridZ + 1], 1),
                    new Vector3f(0, heights[gridX][gridZ + 1],1),
                    new Vector2f(xCoord, zCoord));
        }
        return answer;
    }

//    generate Terrain from height map, shape of terrain etc.
    private RawModel generateTerrain(Loader loader, String heighMap){

//        read heightmap
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("res/" + heighMap + ".png"));
        }catch (IOException e){
            e.printStackTrace();
        }

        int VERTEX_COUNT = image.getHeight();

        heights = new float[VERTEX_COUNT][VERTEX_COUNT];

//        handle information about map vertex
        int count = VERTEX_COUNT * VERTEX_COUNT;
        float[] vertices = new float[count * 3];
        float[] normals = new float[count * 3];
        float[] textureCoords = new float[count * 2];
        int[] indices = new int[6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT * 1)];
        int vertexPointer = 0;

//        generate all terrain and put information into Vectors
        for(int i=0 ; i < VERTEX_COUNT ; i++){
            for(int j=0 ; j < VERTEX_COUNT ; j++){
                vertices[vertexPointer * 3] = (float)j/((float)VERTEX_COUNT - 1) * SIZE;
                float height = getHeight(j,i,image);
                heights[j][i] = height;
                vertices[vertexPointer * 3 + 1] = getHeight(j,i,image);
                vertices[vertexPointer * 3 + 2] = (float)i/((float)VERTEX_COUNT - 1) * SIZE;
                Vector3f normal = calculateNormal(j,i,image);
                normals[vertexPointer *3 ] = normal.x;
                normals[vertexPointer * 3 + 1] = normal.y;
                normals[vertexPointer * 3 + 2] = normal.z;
                textureCoords[vertexPointer * 2] = (float)j/((float)VERTEX_COUNT - 1);
                textureCoords[vertexPointer * 2 + 1] = (float)i/((float)VERTEX_COUNT - 1);
                vertexPointer++;
            }
        }
        int pointer = 0;
        for(int gz = 0; gz < VERTEX_COUNT - 1 ; gz++){
            for(int gx = 0 ; gx < VERTEX_COUNT - 1 ; gx++){
                int topLeft = (gz * VERTEX_COUNT) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1 ) * VERTEX_COUNT) + gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }
        return loader.loadToVAO(vertices, textureCoords, normals, indices);
    }

//  calculate normal vectors
    private Vector3f calculateNormal(int x, int y, BufferedImage image){
        float heightL = getHeight(x -1,y,image);
        float heightR = getHeight(x + 1, y, image);
        float heightD = getHeight(x, y -1, image);
        float heightU = getHeight(x, y +1, image);
        Vector3f normal = new Vector3f(heightL-heightR,2, heightD-heightU);
        normal.normalize();
        return normal;
    }

//    get information about terrain height from heightMap
    private float getHeight(int x, int y, BufferedImage image){
        if(x < 0 || x >= image.getHeight() || y < 0 || y >= image.getHeight()){
            return 0;
        }
        float height = image.getRGB(x,y);
        height += MAX_PIXEL_COLOUR/2f;
        height /= MAX_PIXEL_COLOUR/2f;
        height *= MAX_HEIGHT;
        return height;
    }

}

























