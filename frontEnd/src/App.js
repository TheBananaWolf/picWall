import React, { useState, useEffect } from "react";

function App() {
  const url = "http://127.0.0.1:8080/showmedia"; // Backend endpoint
  const numberOfCells = 2000; // Total number of cells

  const [mediaItems, setMediaItems] = useState([]);

  useEffect(() => {
    // Apply global styles to remove default margins and paddings
    document.body.style.margin = "0";
    document.body.style.padding = "0";
    document.body.style.overflow = "hidden";

    // Initialize empty cells
    const initialItems = Array.from({ length: numberOfCells }, (_, i) => ({
      id: i,
      src: null, // Placeholder until fetched
      type: null, // Placeholder type
      originalWidth: 0,
      originalHeight: 0,
    }));
    setMediaItems(initialItems);

    // Fetch initial media for each cell
    initialItems.forEach((item) => fetchMediaForCell(item.id));

    // Set interval to refresh each cell every 15 seconds
    const intervalId = setInterval(() => {
      initialItems.forEach((item) => fetchMediaForCell(item.id));
    }, 15000); // 15 seconds

    return () => clearInterval(intervalId); // Clean up on component unmount
  }, []);

  const fetchMediaForCell = async (cellId) => {
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error("Failed to fetch media");

      const contentType = response.headers.get("content-type");
      const mediaBlob = await response.blob();
      const mediaObjectURL = URL.createObjectURL(mediaBlob);

      let mediaType = "image";
      if (contentType.startsWith("video")) mediaType = "video";
      else if (contentType.startsWith("image/gif")) mediaType = "gif";

      // Handle image dimensions dynamically
      if (mediaType === "image" || mediaType === "gif") {
        const img = new Image();
        img.src = mediaObjectURL;
        img.onload = () => {
          const { naturalWidth, naturalHeight } = img;

          // Update the specific cell with correct dimensions
          setMediaItems((prevItems) =>
            prevItems.map((item) =>
              item.id === cellId
                ? {
                    ...item,
                    src: mediaObjectURL,
                    type: mediaType,
                    originalWidth: naturalWidth,
                    originalHeight: naturalHeight,
                  }
                : item
            )
          );
        };
      }
    } catch (error) {
      console.error(`Error fetching media for cell ${cellId}:`, error);
    }
  };

  const renderMedia = (item) => {
    if (!item.src) {
      return <div style={styles.placeholder}>Loading...</div>; // Placeholder while loading
    }

    const aspectRatio = item.originalHeight / item.originalWidth;

    switch (item.type) {
      case "video":
        return (
          <video
            src={item.src}
            controls
            autoPlay
            loop
            muted
            style={styles.media}
          />
        );
      case "gif":
      case "image":
        return (
          <img
            src={item.src}
            alt={`media-${item.id}`}
            style={{
              ...styles.media,
              height: "100%",
              width: "100%",
            }}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div style={styles.masonryContainer}>
      {Array.from({ length: 8 }).map((_, columnIndex) => (
        <div key={columnIndex} style={styles.masonryColumn}>
          {mediaItems
            .filter((_, index) => index % 8 === columnIndex)
            .map((item) => (
              <div
                key={item.id}
                style={{
                  ...styles.gridItem,
                  marginBottom: "8px", // Spacing between items in the column
                 
                }}
              >
                {renderMedia(item)}
              </div>
            ))}
        </div>
      ))}
    </div>
  );
}

const styles = {
  masonryContainer: {
    display: "flex",
    justifyContent: "center",
    gap: "8px", // Spacing between columns
    width: "100vw",
    height: "100vh",
    padding: "16px",
    boxSizing: "border-box",
    overflowY: "auto", // Enable vertical scrolling
  },
  masonryColumn: {
    flex: "1",
    display: "flex",
    flexDirection: "column",
  },
  gridItem: {
    position: "relative",
    background: "#f9f9f9",
    borderRadius: "4px",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
  },
  media: {
    objectFit: "cover", // Ensures the media fits within the box
  },
  placeholder: {
    color: "#aaa",
    fontSize: "14px",
  },
};

export default App;
