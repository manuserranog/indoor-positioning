
package com.manuelserrano.tfg.models;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BeaconPage {

    @SerializedName("results")
    @Expose
    private List<BeaconBuilding> results = null;
    @SerializedName("page")
    @Expose
    private Integer page;
    @SerializedName("limit")
    @Expose
    private Integer limit;
    @SerializedName("totalPages")
    @Expose
    private Integer totalPages;
    @SerializedName("totalResults")
    @Expose
    private Integer totalResults;

    public List<BeaconBuilding> getResults() {
        return results;
    }

    public void setResults(List<BeaconBuilding> results) {
        this.results = results;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

}
