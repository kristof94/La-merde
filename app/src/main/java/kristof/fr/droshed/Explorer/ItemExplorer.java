package kristof.fr.droshed.Explorer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kristof
 * on 4/23/17.
 */

public abstract class ItemExplorer implements Parcelable{



    protected String type;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    protected String path;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemExplorer that = (ItemExplorer) o;

        if (layoutID != that.layoutID) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + layoutID;
        return result;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected String name;
    protected int layoutID;

    public ItemExplorer(String type, String name, int layoutID,String path) {
        this.type = type;
        this.name = name;
        this.layoutID = layoutID;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return toString();
    }

    public int getLayoutID() {
        return layoutID;
    }

    @Override
    public abstract String toString();

    protected ItemExplorer(Parcel in) {
        this.type = in.readString();
        this.name = in.readString();
        this.layoutID = in.readInt();
        this.path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(name);
        dest.writeInt(layoutID);
        dest.writeString(path);
    }


}